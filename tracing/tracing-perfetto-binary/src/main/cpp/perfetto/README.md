Perfetto SDK (source code): a complete snapshot of Perfetto SDK.

# Updating

Pick a release tag from https://github.com/google/perfetto/releases. In the example below we will
use v22.1. Then follow the snippet below.
```
tag="v22.1" &&
tmp="perfetto-$(date +%s)" &&
git clone https://github.com/google/perfetto.git $tmp && cd $tmp &&
git checkout "$tag" && version=$(git log --pretty=oneline | head -1) &&
cp sdk/* ../ && cd .. && rm -rf $tmp &&
echo "Version: $version" &&
echo "$version" >> README.md
```

# Last import

aeaeee0ef52d2433e1e20b3d3cc91bfc89d34a91 Amalgamated source for v22.1
