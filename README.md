# Organizer

This is a command line tool I created to sort files into a directory structure based on the files date. It was mainly designed for organizing images (jpg, jpeg, ...) and videos (mp4, mov, avi, ...). It reads the date from the file's metadata if it exists (https://github.com/drewnoakes/metadata-extractor) , as in jpg, mp4, mov, etc. files. Or else it accesses the 'Last Modified' field instead. It's implemented in Java because I like Java. The interface is heavily inspired by how git works.

The program maintains a file structure where files from the same date are located in the same directory. Each folder has a certain threshold. If exceeded all the files are moved into a lower layer. The layers indicate time units in decreasing order. They go from year --> month --> day of month --> hour --> minute --> second.

In the example below, the threshold is equal to <b>1</b>. Here you can see how the files are organized and how the folders are named can be seen. Before, there are some text files and a csv file.
```
E:.
├───exampleFiles
│   ├───csv
│   │       testCsv.csv
│   │
│   └───txt
│           test0.txt
│           test1.txt
│           test2.txt
│           test3.txt
│           test4.txt

```

And this is how the folder structure looks like after organization. See how each folder has only <b>1</b> single file.

```
E:.
└───repo
│   ├───2010
│   │       test2.txt
│   │
│   ├───2021
│   │       test4.txt
│   │
│   ├───2023
│   │   ├───2023_feb
│   │   │       test1.txt
│   │   │
│   │   └───2023_märz
│   │       ├───2023_märz_17
│   │       │       test3.txt
│   │       │
│   │       └───2023_märz_21
│   │               test0.txt
│   │
│   └───error

```

# Usage and Features

Assuming the directory of the organizer/ folder (can be found in releases) is included in the environment table under Windows. A bat file is included. A similar bash file is still not implemented.

Initialize the repository in the current working directory and define the threshold for each folder. Here, the threshold is 500.

`organizer init 500`

Then, copy any files you want into the repository by using the 'organizer' command and including the directory to the source.

`organizer organize path/to/source`

Alternatively, move the files which is much faster if the repo and the source are on the same disk, by using the option '--move'.

`organizer organize path/to/source --move`

Additional features include checking the repo for errors and inconsistencies via the 'check' command.

`organizer check`

The errors found in 'check' can be fixed by running the 'repair' command.

`organizer repair`

A list of all the commands and available options can be printed with 'help'.

`organizer help`

```
LIST OF COMMANDS:

init             | initialize organizer destination directory, default folder size is 500
                 | usage: init [folderSize]
help             | print help text
repair           | repair the structure if there are any errors
organize         | copy and organize all files according to their time stamp into the repository in the current working directory
                 | usage: organize /path/to/source
setProperty      | set and store property, currently available properties: folderSize=N: set folder size threshold
                 | usage: setProperty [property=value]
check            | check whether the repository structure is consistent
version          | organizer version 0.3
delete           | delete repo in current working directory if it exists
mark             | read the date from all files in the current or the given directory and mark them to improve performance
                 | usage: mark [/optional/directory]
status           | print status information in console

LIST OF OPTIONS:

fileExtensions   | constrain allowed file extensions
                 | usage: --fileExtensions=[jpg,jpeg,png,txt,...]
move             | move files instead of copying them
ignoreMark       | ignores the mark set on the file and read the date from metadata instead
replace          | replace files that already exist instead of skipping them

```