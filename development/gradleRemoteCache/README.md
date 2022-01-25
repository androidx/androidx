# Setting up the Gradle Build Cache Node on Google Cloud Platform.

To setup the [Gradle Remote Cache](https://docs.gradle.com/build-cache-node) you need to do the following:

## Create a new Instance

* Open the Cloud Platform [console](https://console.cloud.google.com/home/dashboard?project=fetch-licenses).

* In the search box type in and select `VM Instances`.

* Click on an existing node to see details page, then use `Create Similar` to create a new node.
  *Note*: This node has to be tagged with a network tag called `gradle-remote-cache-node`
  for it to be picked up by the load balancer. Make sure you create the node in the zone `us-east-1-b`.

* Click `Allow HTTP Traffic` and `Allow HTTPs Traffic`. By doing do, you are allowing UberProxy access
  to the remote cache. The load balancer is only available when you are on a corp network.

* Connect to the newly created node using an SSH session. You can use the `gcloud` CLI for this.
  *Note*: Use the `external` IP of the newly created node to SSH.

```bash
# Note: To switch projects use `gcloud config set project fetch-licenses`
# Will show the newly created instance
gcloud compute instances list
# Will setup ssh configurations
gcloud compute config-ssh
ssh 123.123.123.123
```

## Starting the Gradle Remote Cache Node

```bash
# Install some prerequisite packages
sudo apt update
sudo apt upgrade
sudo apt install openjdk-11-jdk tmux wget
# Create a folder `Workspace` in the home directory.
mkdir Workspace
cd Workspace
mkdir -p data/conf
# using the template in this checkout create config.yaml
vi data/conf/config.yaml
# using the template in this checkout create run_node, replace YOURUSERNAME with your username
vi run_node
chmod +x run_node
mkdir gradle-node
wget https://docs.gradle.com/build-cache-node/jar/build-cache-node-11.1.jar -P gradle-node
# Create a `tmux` session
tmux new -s gradle
sudo ./run_node &
# Detach from the tmux session ctrl+b then d
exit
```

## Update the `gradle-remote-cache-group` instance group.

* Open `Instance groups` in gcloud console
* Click on `gradle-remote-cache-group` and select `Edit Group`.
* Select the new node(s), from the drop-down list.
* Remove old nodes from the list
* Click `Save`.
