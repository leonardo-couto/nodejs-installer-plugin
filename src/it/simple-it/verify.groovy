File node = new File(basedir, "gen/node/bin/node");
File npm = new File(basedir, "gen/node/bin/npm");

assert node.isFile()
assert npm.exists()
