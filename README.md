Shace Event API
=====================================

Installation
-----------------------------------

- Install Play 2.2.x
- Install Postgresql
- Create a database called `shace`
- Copy conf/application.conf.example to conf/application.conf
- By default the API is configured to connect with default postgresql user and adress, you can change configuration in file `conf/application.conf`
- To launch the application in debug mode, go into main folder and type `play debug run`
- You can access and debug the API on `http://localhost:9000/events` for example

Configure an Eclipse project
-----------------------------------

To configure an Eclipse project, type `play eclipse`. Then import the main directory into Eclipse with `import project`

To debug into Eclipse, add breakpoints and show variables, you need to: 
- Launch the server using `play debug run`
- Play will show you something like `Listening for transport dt_socket at address: 9999`
- Go to eclipse -> run -> Debug configurations
- Left click on `Remote Java Application` and select `New`
- Choose the corresponding project `api`, `Standard (socket attach)` connection type, `localhost` in host and `9999` in port.
- Click Debug (now the configuration is accesible with the drop-down button near the little bug)

Now if you place a breakpoint, program will break into your eclipse project.

Running unit tests
-----------------------------------

Tests are not yet available.
Coming soon...

Using Git
-----------------------------------

`master` is the main branch.
At the beginning we will not create branch, to speed up global project creation.
Next for each big features, you have to create a branch with the name of the feature, for example `media-upload`
For minor correction (one commit, few lines changed), you can work directly on `master` 

Get a synchronized version of the repository:
- `git clone https://github.com/ShaceEvent/api.git`

Get last version of current branch:
- `git pull`

Create a branch:
- `git checkout -b <new branch>`

Show current branch:
- `git branch -a`

Switch branch (commit your changes before !):
- `git checkout <branch>`

Commit modification on current branch:
- on each file you want to commit `git add <file>` (be careful !)
- `git commit -m <a very explicit message in english>` (No messages like `test`, `commit`, `42`, `boobs`, ...)
- `git pull`, this will merge your current changes with last version before pushing.
- `git push`

Merge current branch with another :
- `git merge <other branch>`, never merge another branch into master ! Merge master into your branch first, next I will merge your branch into master.

If one or more conflicts appear during merge:
- `git status` to show all conflicting files
- `git mergetool` this will ask you for each conflict the action. If you want to merge both files, press enter and git will open default program for merge (which is probably vi or emacs). To specify a tool, use `git mergetool -t <toolname>` (meld is a good one).

If a file with only local modification (database configuration for example) is in conflict you can:
- `git stash` to put modifications in a local "box" while merging.
- `git stash apply` after merging to reapply local changes

Warning ! For little feature or bug correction on `master` branch do `git pull --rebase` instead of `git pull`.

Be really careful with git, if you have any doubt about any command, ask before !

More
-----------------------------------

For more information, contact loick.michard@gmail.com

Kindness and chocolate <3
