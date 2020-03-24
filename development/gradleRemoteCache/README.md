# Setting up the Gradle Build Cache Node on Google Cloud Platform.

To setup the [Gradle Remote Cache](https://docs.gradle.com/build-cache-node) you need to do the following:

## Create a new Instance

* Open the Cloud Platform [console](https://console.cloud.google.com/home/dashboard?project=fetch-licenses).

* Select `Compute Engine Instances`.

* Click on an existing node, and use `Create Similar` to create a new node.
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
```

## Starting the Gradle Remote Cache Node

* Create a folder `Workspace` in the home directory.
* Copy the contents of this folder, into the `Workspace` folder. Remove the `.empty` file from the `data` folder.
* Download the build cache [jar](https://docs.gradle.com/build-cache-node/jar/build-cache-node-9.0.jar) and
  copy it into the `gradle-node` folder.
* Create a `tmux` session using `tmux new -s gradle`.
* Run `chmod +x ~/Workspace/run_node`.
* Install JDK 11 using `sudo apt install openjdk-11-jdk`.
* Finally run the `run_node` script using `sudo ./run_node` from inside the `tmux session`.
* Detach from the `tmux session`.

## Update the `gradle-remote-cache-group` instance group.

* Add the newly created node, to the instance group by using `Edit` instance group.
* Select the node, from the drop-down list.
* Click `Save`.
