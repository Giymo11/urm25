# urm25

# Hobby Tracker

to set up, install coursier, run `cs setup` and install mill

to generate the schedule, run:
```bash
scala scripts/gen_schedule.sc
```
this will create a schedule.json file. You can see an example in `/data`.

to start the server, run:
```bash
./mill server.run
```
then open your browser at http://localhost:8080

After building the docker image, you can also use docker compose to set it up with an existing caddy reverse proxy:
```bash
docker compose up --build
```

## backing up server log:

The server will log access into a `server.log` file, for later analysis. An example can be seen in `/data`.

to back it up, use:
```bash
scp user@domain:~/urm25/data/server.log server.log.backup
```

# quantitative

All data related to the quantitative analysis of the study is contained in the `quantitative/` folder.

# qualitative

All materials to replicate the qualitative analysis are contained in the `qualitative/` folder.



