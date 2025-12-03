# urm25

to set up, install coursier, run `cs setup` and install mill

to generate the schedule, run:
```bash
scala scripts/gen_schedule.sc
```
this will create a schedule.json file.

to start the server, run:
```bash
./mill server.run
```
then open your browser at http://localhost:8080

## backing up server log:
```bash
scp ubuntu@wasabi.science:~/urm25/data/server.log server.log.backup
```



