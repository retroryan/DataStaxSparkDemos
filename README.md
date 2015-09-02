## DataStax Enterprise Platform Workshop

![](http://www.datastax.com/wp-content/themes/datastax-2014-08/images/common/logo.png)

Ryan Knight
[DataStax](http://datastax.com)

[@knight_cloud](https://twitter.com/knight_cloud)

## Setup Instructions

The workshop requires the following software:
* Java 8
* Install [Maven](https://maven.apache.org/download.cgi)
* Install DataStax Enterprise 4.7

### Install & configure DSE

* Download the latest [DataStax Enterprise 4.7 Release](www.datastax.com/downloads)
* Follow the Installation Guide at [Installing DataStax Enterprise](http://docs.datastax.com/en/datastax_enterprise/4.7/datastax_enterprise/install/installTOC.html)
  * There are a number of different install options including a virtual machine or plain tarball install.
  * A tarball can be downloaded from the [Enterprise Downloads](http://downloads.datastax.com/enterprise/)
* Create a single node cluster either on your local laptop, a virtual machine or a cloud server.
  * A tarball install is as simple as unzipping it and running DSE.
* If you are running in the cloud (like Amazon EC2 or GCE) and need to connect to Cassandra from your laptop you will need to set the IP addresses to be the external IP. In the Cassandra.yaml set the following to the correct external IP addresses:
  * Listener address: 127.0.0.1
  * RPC address: 127.0.0.1
* Ensure that user that runs DSE has write permissions to the  data and log directories.
  * For a package installation (these are the directories) [http://docs.datastax.com/en/datastax_enterprise/4.7/datastax_enterprise/config/refDsePackageLoc.html]
  * For a tarball install these are the default directories used:
    * Cassandra data files /var/lib/cassandra
    * Spark data file location to /var/lib/spark
    * Set the log file locations with correct write permissions:
    * Cassandra log files /var/log/cassandra
    * Spark log files /var/log/spark
* Running DSE from a terminal window with Spark and Search enabled (tarball install):
  * cd to the dse directory (i.e. cd ~/dse-4.7.0)
  * run DSE in the foreground with Spark Enabled:  bin/dse cassandra -k -f
  * -f runs cassandra in the foreground so you can see the log output and easily kill the process
* Running DSE with Spark and Search from a package install [see these docs](http://docs.datastax.com/en/datastax_enterprise/4.7/datastax_enterprise/spark/sparkStart.html)
* Verify cqlsh is running.  In a separate terminal window:
  * cd to the dse directory (i.e. cd ~/dse-4.7.0)
  * run cqlsh:   bin/cqlsh localhost
* Verify the Spark Master is up by going to [url of the spark master](http://localhost:7080/)
* It is useful for running spark jobs to add dse to the path.  For example add dse-4.7.3/bin to your path.

The following examples are available in this demo:

[Intro to Spark](INTRO_README.md)

[Intro to Spark Streaming](STREAMING_EXERCISES.md)

[Spark Streaming with Kafka](STREAMING_EXERCISES.md)
