# BTRBCK

Backup tool leveraging BTRFS for incremental backups. Key features:

* **Atomic Copy**: No need to shut your servers down for backup
* **Incremental Snapshots**: Take incremental snapshots,
freely configure how long they are kept.
* **Snapshot transfer via SSH**: Widespread and secure way of data transfer.
* **Simple Command Line Tool**: Easy to setup, easy to use.
* **Look at your Snapshots any Time**: Snapshots are directly visible in the
file system.

## Introduction
Traditional backup systems have two inherent pain points which
are solved by the use of BTRFS snapshots for backup:

1. **No Atomic Snapshots**: The files are copied one by one. To take
a consistent snapshot, your application has to be stopped before the
backup. This makes the backup process more difficult to set up and 
prohibits frequent backups. Since BTRFS snapshots are atomic, snapshots
can be taken at any time. If the snapshot is restored, it looks to the
application as if a power loss had occurred.

2. **Cumbersome Incremental Snapshots**: Managing a combination of 
full and incremental backups is complicated and the restore time
might suffer. With BTRBCK, the snapshots on the backup server are kept within
a BTRFS file system. Snapshots just share common data and store the 
differences at a block level. There its no distinction between full and 
incremental snapshots. The time penalty of restoring a full backup an applying
tens of incremental backups goes away. Any snapshot can be deleted,
without affecting any other snapshot.

BTRBCK works with so called snapshot streams. All data of an application (Database, File store, ...)
has to be placed in the working directory of a single stream. Then snapshots of the working directory
can be taken and transferred to another system.

Streams are organized in stream repositories. This allows to share some configuration
(like the destination host to push snapshots to) and to perform bulk operations (take a snapshot for
each stream in the repository). There are two types of stream repositories: application stream repositories, 
which contain a working directory for each stream, and backup stream repositories, which do not contain working
directories. Besides this, the two repository types have identical functionality.
Backup stream repositories are intended to be used on backup hosts, where no working directories are required, while
application stream repositories are required to run applications.

To set up a backup, your server and your backup host need to use a BTRFS file system. Create an application
stream repository on the server and a backup stream repository on the backup host. Now you can create streams
on the server and sync them to the backup host.

## Getting Started
BTRBCK is distributed as debian package or as executable jar. Releases can be found under https://github.com/ruediste1/btrbck/releases.
Install the package using

    wget https://github.com/ruediste1/btrbck/releases/download/0.9/btrbck-cli_1.0.SNAPSHOT_all.deb && sudo dpkg -i btrbck-cli_1.0.SNAPSHOT_all.deb
    
    wget https://github.com/ruediste1/btrbck/releases/download/1.0/btrbck-cli_1.0_all.deb && sudo dpkg -i btrbck-cli_1.0_all.deb

Now you can create your first stream repository. First, open a root shell

    sudo su
    
Create an empty directory on a BTRFS file system and run

    btrbck create -a

Followed by

    btrbck create myStream

to create your first stream. This created a `myStream` directory in the repository. Create a file and take a snapshot:

    echo "Hello" > myStream/world.txt
    btrbck snapshot myStream
    
Running `find .` you can see the working directory and the snapshot which has been taken. Delete the file and take a snapshot again

    rm myStream/world.txt
    btrbck snapshot myStream
    
Now, you can restore the file using

    btrbck restore myStream 0

To conclude this section, setup BTRBCK on a second system. Create a stream repository and setup ssh with public key authentication
for the root user. Then run 
    
    btrbck push myStream root@<host> <remote repo path>
    
This will transfer all snapshots to the remote host.

## Permissions
To operate, BTRBCK needs to invoke the `btrfs` and `chown` commands as super user. This 
also includes invocation on remote systems (`push` and `pull` to/from other hosts), where
sudo has to be set up in password less configuration. This can be accomplished by 

1. running `btrbck` as root
1. using `sudo btrbck`
1. instructing `btrbck` to use sudo commands

Variant 1 is the simplest and needs no further explanation. For variant 2, you have to use
the `-sudoRemoteBtrbck` flag and add an `/etc/sudoers.d/btrbck` file with the following contents:

    %sudo	ALL = (ALL) NOPASSWD: /usr/bin/btrbck

For variant 3, add a `/etc/sudoers.d/btrbck` file with the following. In addition, the
`-sudo` and `-sudoRemoteBtrfs` flags have to be specified on the command line.

    %sudo	ALL = (ALL) NOPASSWD: /sbin/btrfs
    %sudo	ALL = (ALL) NOPASSWD: /bin/chown

## Snapshot Retention
Taking snapshots is all good an fine, but at some time you'll want to thin them out. If snapshots are taken every 10 minutes,
a snapshot retention configuration might look as follows:

* keep all snapshots for one hour
* keep one snapshot per hour for one day
* keep 4 snapshots a day for a week
* keep one snapshot a week for a month
* keep one snapshot a month for a year
* keep one snapshot per year for 10 years.
* keep one snapshot per decade for 100 years.

This can be configured on a per stream level. The stream configuration file can be found in `.backup/<stream name>/<stream name>.xml`
for application stream repositories and in `<stream name>/<stream name>.xml` in backup stream repositories. The following is
the configuration for the example above:

    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <stream initialRetentionPeriod="PT1H" snapshotInterval="PT10M">
      <retention period="P1D" timeUnit="HOUR" snapshotsPerTimeUnit="1"/>
      <retention period="P1W" timeUnit="DAY" snapshotsPerTimeUnit="4"/>
      <retention period="P1M" timeUnit="WEEK" snapshotsPerTimeUnit="1"/>
      <retention period="P1Y" timeUnit="MONTH" snapshotsPerTimeUnit="1"/>
      <retention period="P10Y" timeUnit="YEAR" snapshotsPerTimeUnit="1"/>
      <retention period="P100Y" timeUnit="DECADE" snapshotsPerTimeUnit="1"/>
    </stream>

The `snapshotIterval` will be discussed below. All Periods are specified according to [ISO 8601](http://en.wikipedia.org/wiki/ISO_8601).
Basically, the format is `PnYnMnDTnHnMnS`, where zeros can be omitted and the `T` is used to indicate the start of the time part.

## Automatic Processing