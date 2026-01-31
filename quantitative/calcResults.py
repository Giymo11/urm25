#import numpy as np  
import csv
import json
#from scipy import stats  

#list a_days 
#list b_days

def mapLogsToUser(path):
    users = {}
    with open(path) as f:
        reader = csv.DictReader(f)
        for row in reader:
            user = row["nickname"]
            if users.get(user) == None :
                users.update({user: []})
        
            user_entries = users[user]
            user_entries.append(row)
    return users
    
def countUserEntries(users: dict):
    sorted_users = sorted(users.items(), key = lambda x: len(x[1]))  
    totalEntries = 0

    for username, entries in sorted_users:
        print(username, len(entries))
        totalEntries += len(entries)
 
    print("total entries: ", totalEntries)


def addSchedules(users: dict, path):
    retUsers = {}
    with open(path) as f:
        j = json.loads(f)
        for entry in j["schedules"]:
            name = entry["nickname"]
            if users.get(name) is not None:
                retUsers.update({name: {"schedule" : entry["pattern"],"daily_entries" : users.get(name)}})
    return retUsers    



def addLogs(users: dict):
    retUsers = {}
    with open("./server_logs.csv") as f:
        j = json.loads(f)
        for entry in j["schedules"]:
            name = entry["nickname"]
            retUsers.update({name: {"schedule" : entry["pattern"],"daily_entries" : users.get(name)}})
    return retUsers  

def buildDataForStatistics(): list




def two_sample_t_test(N1, N2, M1, M2):
    # Sample Sizes
    #N1, N2 = 21, 25

    # Degrees of freedom  
    dof = min(N1,N2) - 1

    # Gaussian distributed data with mean and var = 1  
    x = np.random.randn(N1) + M1

    # Gaussian distributed data with mean and var = 1  
    y = np.random.randn(N2) + M2

    ## Using the internal function from SciPy Package  
    t_stat, p_val = stats.ttest_ind(x, y)  
    print("t-statistic = " + str(t_stat))  
    print("p-value = " + str(p_val))




def main():

    users = buildDataForStatistics()

    sample_dict = { 
        "daniel": {
            "schedule": "AABAB",
            "entries": ["a", "b", "c"]
        }
    }

    print(sample_dict)

    entry = sample_dict.get("daniel")
    entry["xpp"] = "xdd"
    sample_dict["daniel"] = entry

    print(sample_dict)




if __name__=="__main__":
    main()
