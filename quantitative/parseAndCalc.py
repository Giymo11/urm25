import statistics
#import numpy as np  
import csv
from datetime import datetime, timedelta
import json
from scipy import stats  

def generateUserList():
    users = {}
    with open("schedule.json") as f:
        j = json.load(f)
        for row in j["schedules"]:
            user = row["nickname"]
            users.update({user: {
                "schedule" : "",
                "logs": [],
                "dailyQ": [],
                "preQ": "",
                "postQ": ""
            }})
    return users

def parseDailyQuestionnaire(users: dict):
    with open("./daily_questionnaire.csv") as f:
        reader = csv.DictReader(f)
        for row in reader:
            user = row["nickname"]
            entry = users.get(user)
            entry["dailyQ"].append(row)
            users[user] = entry 
    
def parseSchedules(users: dict):
    with open("schedule.json") as f:
        j = json.load(f)
        for row in j["schedules"]:
            name = row["nickname"]
            if users.get(name) != None :
                entry = users.get(name)
                entry["schedule"] = row["pattern"]
                users[name] = entry
            
def parseLogs(users: dict):
    with open("./server_logs.csv", "r", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f, delimiter=',')
        for row in reader:
            name = row["nickname"][1:]
            if users.get(name) != None :
                entry = users.get(name)
                entry["logs"].append(row)
                users[name] = entry

def parsePostandPreQ(users:dict):
    with open("./post_questionnaire.csv") as f:
        reader = csv.DictReader(f)
        for row in reader:
            name = row["nickname"]
            if users.get(name) != None :
                entry = users.get(name)
                entry["postQ"] = row
                users[name] = entry
    
    with open("./pre_questionnaire.csv") as f:
        reader = csv.DictReader(f)
        for row in reader:
            name = row["nickname"]
            if users.get(name) != None :
                entry = users.get(name)
                entry["preQ"] = row
                users[name] = entry

def printDailyQEntries(users: dict):
    #sorted_users = sorted(users.items(), key = lambda x: len(x["dailyQ"]))  
    totalEntries = 0

    for user in users:
        u = users.get(user)
        print(user, len(u["dailyQ"]))
        totalEntries += len(u["dailyQ"])
    print("total entries", totalEntries)
 
def printUserLogs(users:dict):
    totalEntries = 0
    for user in users:
        u = users.get(user)
        print(user, len(u["logs"]))
        totalEntries += len(u["logs"])
    print("total logs", totalEntries)

def printUserData(users:dict):
    for user in users:
        u = users.get(user)
        print(user, "\n\tschedule:", u["schedule"], 
              "logs: ", len(u["logs"]), 
              "dailyQ: ", len(u["dailyQ"]),
              "preQ: ", "yes" if u["preQ"] != "" else "no",
              "postQ: ", "yes" if u["postQ"] != "" else "no",
              "\n"
              )

def parseData():
    users = generateUserList()
    parseDailyQuestionnaire(users)
    parseSchedules(users)
    parseLogs(users)
    parsePostandPreQ(users)
    return users

def getDailyGroupSamplesFromLog(users: dict):
    dailyMinutes = []
    dailySessions = []
    for u in users:
        user = users.get(u)
        dailyMinsA = 0
        dailyMinsB = 0
        dailySessA = 0
        dailySessB = 0
        occA = 0
        occB = 0
        for i, cat in enumerate(user["schedule"]):
            logsForDay = getLogsForDay(user["logs"], i+1)
            sessions, mins = calcMinutesAndSessionsPerDay(logsForDay)
            if cat == "A":
                dailyMinsA += mins
                dailySessA += sessions
                occA += 1
            if cat == "B":
                dailyMinsB += mins
                dailySessB += sessions
                occB += 1
        dailyMinutes.append((u, dailyMinsA / occA, dailyMinsB /occB))
        dailySessions.append((u, dailySessA / occA, dailySessB / occB))
    return dailyMinutes, dailySessions

def timestampFromLog(log):
    return datetime.fromisoformat(log["timestamp"][:-9])
                
def getLogsForDay(logs: list, day: int):
    if day > 5 or day < 1:
        print("day not correct")
        return []
    start = datetime.fromisoformat("2025-12-01T03:00:00Z") + timedelta(days=day-1)
    end = start + timedelta(days=1)
    dayLogs = []
    for log in logs:
        timestamp = timestampFromLog(log)
        if timestamp > start and timestamp < end:
            dayLogs.append(log)
    return dayLogs

def calcMinutesAndSessionsPerDay(logs: list):
    tracking_logs = filter(lambda l: l["event"] == " START_TRACKING" or l["event"] == " STOP_TRACKING", logs)
    last_start = None
    sessions = 0
    seconds = 0
    # we ignored cases where "START_TRACKING" or "STOP_TRACKING" occured multiple times in a row
    # if this had occured, we would have seen a "WARN" in the logs
    # we discarded starts where users did not stop
    for (i, log) in enumerate(list(tracking_logs)):
        if log["event"] == " START_TRACKING":
            last_start = timestampFromLog(log)
            sessions += 1
        if log["event"] == " STOP_TRACKING":
            if last_start is not None:
                seconds += (timestampFromLog(log) - last_start).total_seconds()
                last_start = None
    return sessions, seconds / 60

def getDailyGroupSamplesFromQuestionaire(users: dict):
    element = "dailyQ"
    motivated = []
    comfortable = []
    content = []
    avoidTasks =[]
    for u in users:
        user = users.get(u)
        offset = 0

        motivatedA = []
        motivatedB = []
        comfortableA = []
        comfortableB = []
        contentA = []
        contentB = []
        avoidTasksA = []
        avoidTasksB = []

        if u == "eager-fox": # eager-fox did not fill in any daily questionnaires
            continue

        for i, cat in enumerate(user["schedule"]):
            if u == "proud-rabbit" and i == 4: # proud-rabbit did not fill in the questionnaire on day 5
                continue
            
            if u == "lively-lion" and i == 3: # lively-lion did not fill in the questionnaire on day 4
                offset = 1
                continue
            pos = i - offset
            #print(pos, u)
            if cat == "A":
                motivatedA.append(convertFeelingToNumerical(user[element][pos]["motivated"]))
                comfortableA.append(convertFeelingToNumerical(user[element][pos]["comfortable"]))
                contentA.append(convertFeelingToNumerical(user[element][pos]["content"]))
                avoidTasksA.append(convertLikertToNumerical(user[element][pos]["avoided_tasks"]))
            if cat == "B":
                motivatedB.append(convertFeelingToNumerical(user[element][pos]["motivated"]))
                comfortableB.append(convertFeelingToNumerical(user[element][pos]["comfortable"]))
                contentB.append(convertFeelingToNumerical(user[element][pos]["content"]))
                avoidTasksB.append(convertLikertToNumerical(user[element][pos]["avoided_tasks"]))
        nonNegative = lambda l: list(filter(lambda x: x >= 0, l))
        if not (nonNegative(motivatedA) == [] or nonNegative(motivatedB) == []):
            motivated.append((u, statistics.mean(nonNegative(motivatedA)), statistics.mean(nonNegative(motivatedB))))
        else:
            print(u, "bogos pinted")
        if not (nonNegative(comfortableA) == [] or nonNegative(comfortableB) == []):
            comfortable.append((u, statistics.mean(nonNegative(comfortableA)), statistics.mean(nonNegative(comfortableB))))
        if not(nonNegative(contentA) == [] or nonNegative(contentB) == []):
            content.append((u, statistics.mean(nonNegative(contentA)), statistics.mean(nonNegative(contentB))))
        if not (nonNegative(avoidTasksA) == [] or nonNegative(avoidTasksB) == []):
            avoidTasks.append((u, statistics.mean(nonNegative(avoidTasksA)), statistics.mean(nonNegative(avoidTasksB))))

    return [motivated, comfortable, content, avoidTasks]


def getDailyGroupSamplesFromPrePostQ(users: dict):
    element = "preQ"
    comf_taking_time = []
    comfortable = []
    content = []
    avoidTasks =[]
    for u in users:
        user = users.get(u)
        offset = 0

        motivatedA = []
        motivatedB = []
        comfortableA = []
        comfortableB = []
        contentA = []
        contentB = []
        avoidTasksA = []
        avoidTasksB = []

        if u == "eager-fox": # eager-fox did not fill in any daily questionnaires
            continue

        for i, cat in enumerate(user["schedule"]):
            if u == "proud-rabbit" and i == 4: # proud-rabbit did not fill in the questionnaire on day 5
                continue
            
            if u == "lively-lion" and i == 3: # lively-lion did not fill in the questionnaire on day 4
                offset = 1
                continue
            pos = i - offset
            #print(pos, u)
            if cat == "A":
                motivatedA.append(convertFeelingToNumerical(user[element][pos]["motivated"]))
                comfortableA.append(convertFeelingToNumerical(user[element][pos]["comfortable"]))
                contentA.append(convertFeelingToNumerical(user[element][pos]["content"]))
                avoidTasksA.append(convertLikertToNumerical(user[element][pos]["avoided_tasks"]))
            if cat == "B":
                motivatedB.append(convertFeelingToNumerical(user[element][pos]["motivated"]))
                comfortableB.append(convertFeelingToNumerical(user[element][pos]["comfortable"]))
                contentB.append(convertFeelingToNumerical(user[element][pos]["content"]))
                avoidTasksB.append(convertLikertToNumerical(user[element][pos]["avoided_tasks"]))
        nonNegative = lambda l: list(filter(lambda x: x >= 0, l))
        if not (nonNegative(motivatedA) == [] or nonNegative(motivatedB) == []):
            motivated.append((u, statistics.mean(nonNegative(motivatedA)), statistics.mean(nonNegative(motivatedB))))
        else:
            print(u, "bogos pinted")
        if not (nonNegative(comfortableA) == [] or nonNegative(comfortableB) == []):
            comfortable.append((u, statistics.mean(nonNegative(comfortableA)), statistics.mean(nonNegative(comfortableB))))
        if not(nonNegative(contentA) == [] or nonNegative(contentB) == []):
            content.append((u, statistics.mean(nonNegative(contentA)), statistics.mean(nonNegative(contentB))))
        if not (nonNegative(avoidTasksA) == [] or nonNegative(avoidTasksB) == []):
            avoidTasks.append((u, statistics.mean(nonNegative(avoidTasksA)), statistics.mean(nonNegative(avoidTasksB))))

    return [motivated, comfortable, content, avoidTasks]


def convertFeelingToNumerical(answer: str):
    if answer == "Very slightly or not at all":
        return 0
    elif answer == "A little":
        return 1
    elif answer == "Moderately":
        return 2
    elif answer == "Quite a bit":
        return 3
    elif answer == "Extremely":
        return 4
    else:
        if not answer == "I did not pursue any hobbies today.":
            print(f"feeling {answer} was not defined in answer scale")
        return -1
    
def convertLikertToNumerical(answer: str): 
    answers = ["Strongly disagree", "Disagree", "Neutral", "Agree", "Strongly agree"]
    try:
        return answers.index(answer)
    except ValueError:
        return -1

def pairedTTestDailyMinutes(users):
    minutes, _ = getDailyGroupSamplesFromLog(users)
    minutesA = [t[1] for t in minutes]
    minutesB = [t[2] for t in minutes]
    #print(sum(minutesA)/20,  sum(minutesB) / 20) # getting the average numbers
    tTestResult = stats.ttest_rel(minutesA, minutesB)
    pValue = tTestResult.pvalue
    print("p-value + ", pValue)
    #print(minutesA)
    #print(minutesB)

def pairedTtestSessions(users: list):
    _, sessions = getDailyGroupSamplesFromLog(users)
    sessionsA = [t[1] for t in sessions]
    sessionsB = [t[2] for t in sessions]

    tTestResult = stats.ttest_rel(sessionsA, sessionsB)
    pValue = tTestResult.pvalue
    print("p-value + ", pValue)

def wilcoxSessions(users: list):
    _, sessions = getDailyGroupSamplesFromLog(users)
    sessionsA = [t[1] for t in sessions]
    sessionsB = [t[2] for t in sessions]
    coxResult = stats.wilcoxon(sessionsA, y = sessionsB)
    print(coxResult)


def wilcoxSentimentsAndMotivation(users: list):
    res = getDailyGroupSamplesFromQuestionaire(users)
    sentiments = ["motivated", "comfortable", "content", "avoidTasks"]

    for i, sentiment in enumerate(res):
        sentimentA = [s[1] for s in sentiment]
        sentimentB = [s[2] for s in sentiment]
        coxResultMotivated = stats.wilcoxon(sentimentA, y = sentimentB)
        print(sentiments[i],"p-Value: ", coxResultMotivated.pvalue)


def pairedTtestSentimentsAndMotivation(users: list):
    res = getDailyGroupSamplesFromQuestionaire(users)
    sentiments = ["motivated", "comfortable", "content", "avoidTasks"]

    for i, sentiment in enumerate(res):
        sentimentA = [s[1] for s in sentiment]
        sentimentB = [s[2] for s in sentiment]
        tResult = stats.ttest_rel(sentimentA,sentimentB)
        print(sentiments[i],"p-Value: ", tResult.pvalue)
    

def main():

    # parsing test data from logs
    users = parseData()
    #printUserData(users)


    print("daily minutes t test")
    pairedTTestDailyMinutes(users)
    
    print("daily sessions t test")
    pairedTtestSessions(users)

    print("daily sentiments t test")
    pairedTtestSentimentsAndMotivation(users)

    
    

if __name__=="__main__":
    main()
