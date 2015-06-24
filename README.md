nodejs-installer-plugin
=======================

A Node.js and npm installer plugin for Maven.
Currently supports only Linux and MacOS.

**Example application**

https://github.com/leonardo-couto/example-nodejs-plugin

**Usage**

Add the following plugin to your pom.xml

```xml
<project>
  <properties>
    ...
    <node.target>${basedir}/generated</node.target>
    <node.path>${node.target}/node/bin</node.path>
  </properties>
  ...
  <build>
    ...
    <plugins>
      ...
      
      <plugin>
        <groupId>com.github.leonardo-couto</groupId>
        <artifactId>nodejs-installer-plugin</artifactId>
        <version>0.12.5</version>
        <executions>
          <execution>
            <phase>validate</phase>
            <goals>
              <goal>install</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <npm>
            <install>grunt-cli</install>
            <install>karma</install>
          </npm>
          <target>${node.target}</target>
        </configuration>
      </plugin>
      
    </plugins>
  </build>
</project>
```

Node.js and global npm depencencies *grunt-cli* and *karma* will be installed on the *generated* directory of your build in the *validate* phase of your Maven process.

If you don't want Node and global npm dependencies automatically installed when running ```mvn clean install``` remove the *executions* tag and run ```mvn nodejs-installer:install``` for installation.

Node.js binary is cached in your maven local repository and if the specified version is already installed on the *\<target\>* path it doesn't try to install it again.

**Running npm install, Grunt and/or other Node packages**

This plugin only install Node and npm global dependencies. For everything else use something else like *exec-maven-plugin* or *maven-antrun-plugin*. See example below for automatically running ```npm install``` and ```grunt``` using Exec Maven Plugin.


```xml
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>1.3</version>
        <executions>
          <execution>
            <id>npm-install</id>
            <phase>initialize</phase>
            <configuration>
              <executable>${node.path}/npm</executable>
              <arguments>
                <argument>install</argument>
              </arguments>
              <workingDirectory>${basedir}</workingDirectory>
              <environmentVariables>
                <PATH>${node.path}:${env.PATH}</PATH>
              </environmentVariables>
            </configuration>
            <goals>
              <goal>exec</goal>
            </goals>
          </execution>

          <execution>
            <id>grunt-default</id>
            <phase>test</phase>
            <configuration>
              <executable>${node.path}/grunt</executable>
              <workingDirectory>${basedir}</workingDirectory>
              <environmentVariables>
                <PATH>${node.path}:${env.PATH}</PATH>
              </environmentVariables>
            </configuration>
            <goals>
              <goal>exec</goal>
            </goals>
          </execution>

        </executions>
      </plugin>

```

This will run ```npm install``` in initialize phase and ```grunt``` default task in the test phase.
