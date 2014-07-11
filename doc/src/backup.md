% BtrBck - BTRFS Backup Tool

# BtrBck - BTRFS Backup Tool

The backup system provided by the LAP is based on the snapshot feature of 
BTRFS. All data of an application is placed in a single BTRFS subvolume. This
allows instant, atomic snapshots to be taken while the application is running. 
If the application is later restored from the snapshot, it looks to the application
like an ordinary power loss.

The backup tool operates on so called backup streams. All data in a single stream
is managed together. Typically, every application is placed in it's own 
stream. If consistency is required between multiple applications,
all applications have to be placed in the same stream. However, this means also
that the applications have to run on the same server.

Backup streams are contained in stream repositories. There are two types of 
repositories: Application stream repositories contain working
directories upon which applications operate and of which snapshots are 
taken. Backup stream repositories do not contain
working directories and are only used to replicate streams into.

Streams consist of all working directory snapshots and meta data describing the
frequency snapshots are taken, the snapshot retention rules, 
the stream version history.

Streams can be replicated between repositories. Replication is only possible
if a stream is either not contained in the target repository, or if the version
of the stream of the target repository is a parent version of the to be replicated
stream. This avoids overwriting new stream versions with old versions and allows
to detect if multiple application stream repositories are operating on the 
same stream, preventing possible data loss. 

Finally, working directories can be restored from a snapshot.

The following diagram shows a class model of the concepts discussed: 

![Class Overview](backupClasses)

## Versioning
Versioning is used to protect from accidential overwriting of streams. Each stream
has a version. A derived version of a stream can be created by taking a snapshot.
The derived from relation is transitive. A version is always considered to derive
from itself.

Whenever a synchronization between two streams is requested, it is first checked
if the version of the source stream derives from the version of the target stream.

The **VersionHistory** records on which stream instances snapshots were created.
This allows for a very compact representation (instance id with count) as long
as the stream is not transferred frequently between repositories, which is the
typical use case.

In addition, the history records snapshot restore operations.


## Snapshot Retention and Pruning
Snapshots are protected from pruning by **RetentionRules**. Each rule defines a
**PeriodicInstant**. The first snapshot after the instant is protected by the rule.
All snapshots which are not protected are deleted during pruning.

In addition, there is an initial retention period, during which no snapshots are
pruned.  

## Snapshot Clone Sources
Snapshots are transferred as delta to other snapshots. These clone sources are
the previous and the next snapshots available in the target stream. Since the 
snapshots are numbered, this is trivial as long as no restores come into play.

When looking for the next snapshot, restores are not taken into account. When 
looking for the previous snapshot, the restored snapshot is used as previous
snapshot instead of the snapshot which was last created before the snapshot 
to be transferred.  

## Repository Structure
The base directory of a repository contains the configuration and the snapshots.

Backup stream repositories consist solely of the base directory which is also their
root directory. 

The root directory of an application stream repository contains the working
directories of the streams in the repository and a ".backup" directory, 
containing the base directory. The working directories are named after the
respective stream. Each is an individual BTRFS subvolume to allow for atomic 
snapshot.

The location of a repository always points to the root directory. 

The base directory contains a "repository.xml" file, defining the repository name,
the **RemoteRepositories** and the **SyncConfigurations**. 

For each stream in the repository there is a sub directory named after the stream. The
stream directory contains a "stream.xml" file, defining the snapshot frequency,  the initial 
retention period and the **Retentions**. The "versionHistory.xml" file contains the version history. 
There is a "snapshots" directory, containing read only 
BTRFS snapshot subvolumes of the working directory.

In application stream repositories, there is an "instanceId.xml" file, containing the id 
of the stream instance. This is required for clean versioning.

## Active Components
All repository modifications are performed by the "btrbck" command line tool.
Concurrent operations are coordinated by file based locks, either on the
repository or on the stream level.

Periodic activities are driven by the "btrbckd" daemon. Based on the configuration
file it triggers snapshots and synchronizations. To avoid inconsistencies, always 
stop the daemon while editing configuration files.

## Concurrency Control
It needs to be possible to perform the following operations in parallel:
* taking snapshots while synchronizing to another repository
* synchronizing multiple streams from other repositories in parallel
All other operations can well use a global lock.

BTRBCK uses exclusive and shared file locks for concurrency control. There are the
following locks:

* Stream enumeration lock: locks the available streams
* Snapshot creation lock (per stream)
* Snapshot removal lock (per stream)

The daemon acquires a shared stream enumeration lock while it is running. Afterwards 
the configuration is parsed and can only be updated by restarting the daemon.  

## Operations

### Stream Creation
    exclusive lock stream enumeration
    create base directory
    create "stream.xml" with a default configuration
    generate stream id
    if application stream repository
      create working directory subvolume
    unlock stream enumeration 
 
### Stream Deletion
	exclusive lock stream enumeration
	delete base directory
	if application stream repository
	  delete working directory
	unlock stream enumeration
	
### Stream Renaming
	exclusive lock stream enumeration
	rename base directory
	if application stream repository
	  rename working directory
	unlock stream enumeration

### Taking Snapshots
	shared lock stream enumeration
	exclusive lock snapshot creation
	take snapshot
	version.append(this)
	unlock snapshot creation
	unlock stream enumeration
	
### Synchronize to
	shared lock stream enumeration
	call other.synchronizeFrom(this)
	unlock stream enumeration

### Synchronize from
If a new stream is synchronized to a repository, the daemon of the target repository has to be stopped and 
the new stream has to be created manually. 

	shared lock stream enumeration
	exclusive lock snapshot removal
	exclusive lock snapshot creation
	collect snapshot list
	call sendSnapshots
		shared lock stream enumeration
		shared lock snapshot removal
		shared lock snapshot creation
		assert source.versionHistory.isDescendantOf(target.versionHistory)
	    determine snapshots to send and clone from
	    create pruning list
	    unlock snapshot creation
	    create/stream send files
	    stream pruning list 
	    unlock snapshot removal
	    unlock stream enumeration
	receive snapshots
	process pruning list
	receive versionHistory
	receive stream configuration
	unlock snapshot creation
	unlock snapshot removal
    unlock stream enumeration
		

### Pruning Snapshots
	shared lock stream enumeration
	exclusive lock snapshot removal
	exclusive lock snapshot creation
	perform pruning
	unlock snapshot creation
	unlock snapshot removal
	unlock stream enumeration
	
### Restoring Working Directory
	shared lock stream enumeration
	shared lock snapshot enumeration
	restore subvolume
	version.appendRestore()
	unlock snapshot enumeration
	unlock stream enumeration
	
