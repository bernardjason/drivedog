# drivedog

DriveDog is a project I started Nov 2015 to use the 15gb free storage on GoogleDrive in a similar fashion to DropBox from my Linux box. I wanted to copy files to a directory as a backup mechanism and they appear on GoogleDrive. I also wanted to be able to view them via GoogleDrive gui as and when for reference. Same way I can for Dropbox.

Seemed easy enough but it was more invovled than I expected.  This is the first version, I will be using it in anger myself as of 31st December 2015. Providing fixes when i find problems.

Renaming files from GoogleDrive browser gui being reflected on Linux DriveDog directory is still a TODO item. Other operations are supported.

What DriveDog does is create a directory $HOME/drivedog that will mirror your My Drive on GoogleDrive. It will add an icon to the status bar that has options to

* Do a full resync from GoogleDrive to Linux directory rather than changes since last run.
	normally DriveDog uses a marker file called .drivedog_<hostname> to keep track of changes so that it can try and do the minimum work required. This option will remove this file so that DriveDog checks everything.
* Pause DriveDog
	stop what its doing.
* Display a list of recent changes
	A popup gui showing recent pulls and pushes
* Basic help

If running for the first time, run at the command line so that you can authorise on Google permission for DriveDog

java -jar target/scala-2.11/drivedog-assembly-0.1.jar

you will get this like message

  https://accounts.google.com/o/oauth2/auth?access_type=offline&client_id=669332608425-qnjtmnuu275n2ugcb0lklv3e5d6fgo4p.apps.googleusercont..........

use this link to grant permission for drivedog to upload and download files. This is the same granting as other applications wishing to use GoogleDrive must do.

##Running after this
I copied target/scala-2.11/drivedog-assembly-0.1.jar to $HOME/bin

Added this entry to my .bash_profile

nohup java -jar $HOME/bin/drivedog-assembly-0.1.jar 

##Building

sbt assembly

will skip unit tests as assembly.sbt contains

test in assembly := {}

and will create
drivedog/target/scala-2.11/drivedog-assembly-0.1.jar

to run tests

sbt test


##File Conflicts
Conflicts happen when both the local linux drive copy of a file in $HOME/drivedog has been edited and the same file on Google Drive has been edited. DriveDog doesn't know which one to keep.

###Resolve conflict, keep file copy on google drive

remove the file on local storage and the conflict indicator file. For example
rm football.txt
rm .drivedog/.football.txt.conflict

###Resolve conflict, keep local file edit on Linux

Go to Google Drive and remove the file to the bin, then remove the conflict indicator, e.g.
rm .drivedog/.football.txt.conflict

if you remove the file permanently then DriveDog will delete the file locally.
