# System Tracing

## Installation

### Linux

On Linux, System tracing is done using [Perfetto](https://perfetto.dev).

Download the latest Perfetto release from [releases](https://github.com/google/perfetto/releases).

```bash
mkdir perfetto && cd perfetto
# Working directory is $HOME/Tools/perfetto
wget https://github.com/google/perfetto/releases/download/v54.0/linux-amd64.zip
unzip linux-amd64.zip
# Setup
cd linux-amd64
chmod +x *
# Provide the following capabilities to `tracebox` so it can use the relevant trace marker.
sudo setcap 'cap_sys_admin,cap_perfmon+ep' ./tracebox
# Extracts everything into $HOME/Tools/perfetto/linux-amd64
# Add these binaries to the path.
export PATH=$HOME/Tools/perfetto/linux-amd64:$PATH
```
