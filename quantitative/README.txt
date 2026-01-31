In this folder, the data has been edited, which means:

- errors in typing the nickname in the questionnaire such as additional space or misspelling have been polished

- test data surrounding our test user „afghanistan“ has been removed from the server log

- all of the raw data can be found in the folder „unedited data“

- the tables of the questionnaire have been renamed to enhance parsing


- parsed data structure:
 Map of users, the key is the nickname,
 each user has: "schedule" : contains a string of the predetermined schedule (e.g. AABBA),
                "logs": a list containing ALL the logs of the user (csv strings),
                "dailyQ": a list containing ALL the daily questionnaire answers of the user (csv strings),
                "preQ": csv of the pre questionnaire of the user,
                "postQ": csv of the post questionnaire of the user

- it is possible to replicate the tests that have been run using the script. 
  Each test has been extracted in a method, all tests that have been used for the
  statistical analysis are called in the main method. 
  There is additional, unused code for more testing available.
