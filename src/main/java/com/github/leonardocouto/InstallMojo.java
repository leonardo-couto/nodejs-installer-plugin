package com.github.leonardocouto;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Install node if this version is not already installed.
 * Also execute global npm installs (if not already installed)
 */
@Mojo(name = "install", defaultPhase = LifecyclePhase.NONE)
public class InstallMojo extends AbstractMojo {

    @Parameter(property = "npm", defaultValue = "")
    private String[] npm;

    @Parameter(property = "target", defaultValue = "${basedir}/gen")
    private File target;

    @Parameter(property = "binary-group", defaultValue = "${project.groupId}")
    private String binaryGroup;

    @Parameter(property = "binary-artifact", defaultValue = "nodejs-binaries")
    private String binaryArtifact;

    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor plugin;

    @Override
    public void execute() throws MojoExecutionException {

        Artifact binaryArtifact = this.binaryArtifact();
        String version = binaryArtifact.getVersion();

        if (!this.alreadyInstalled(version)) {
            this.install(binaryArtifact);
        }

        List<String> packages = this.nodePackages(version);
        for (String pkg : this.npm) {
            if (!packages.contains(pkg)) {
                this.installPackage(pkg, version);
            }
        }
    }

    private void installPackage(String pkg, String nodeVersion) throws MojoExecutionException {

        getLog().warn("installing npm package " + pkg + " ...");
        String npmPath = this.npmPath(nodeVersion);
        String install = npmPath + " install -g " + pkg;
        Runtime run = Runtime.getRuntime();

        try {
            Process process = run.exec(install);
            this.logInputStream(process.getErrorStream());
            this.logInputStream(process.getInputStream());

        } catch (IOException e) {
            throw new MojoExecutionException("Problems installing node package " + pkg, e);
        }

    }

    private List<String> nodePackages(String version) {
        String template = "%s/%s/lib/node_modules";
        String path = String.format(template, this.target.getPath(), installationDirectoryName(version));
        File nodeModules = new File(path);

        List<String> packages = new ArrayList<>();
        for (File f : nodeModules.listFiles()) {
            packages.add(f.getName());
        }
        return packages;
    }

    private void install(Artifact binaryArtifact) throws MojoExecutionException {
        getLog().warn("installing node.js on " + this.target.getPath() + " ...");
        this.target.mkdirs();

        File file = binaryArtifact.getFile();
        this.unarchive(file, this.target);
        this.renameExtracted(binaryArtifact.getVersion());
    }

    private void renameExtracted(String version) {
        String path = this.target.getPath();

        for (File f : this.target.listFiles()) {
            if (Files.isDirectory(f.toPath()) && (f.getName().startsWith("node-v"))) {
                String newName = path + "/" +  this.installationDirectoryName(version);
                f.renameTo(new File(newName));
                return;
            }
        }
    }

    private boolean alreadyInstalled(String version) throws MojoExecutionException {
        if (!this.target.exists()) return false;

        if (this.target.isFile()) {
            String error = String.format("target '%s' is a file", this.target.toString());
            throw new MojoExecutionException(error);
        }

        for (File f : this.target.listFiles()) {
            if (Files.isDirectory(f.toPath())) {
                String name = f.getName();
                if (this.installationDirectoryName(version).equals(name)) {
                    return true;
                }
            }
        }

        return false;
    }

    private Artifact binaryArtifact() {
        String key = this.binaryGroup + ":" + this.binaryArtifact;
        return this.plugin.getArtifactMap().get(key);
    }

    private String npmPath(String version) {
        return this.baseExecutablePath(version) + "/npm";
    }

    private String baseExecutablePath(String version) {
        String template = "%s/%s/bin/";
        return String.format(template, this.target.getPath(), this.installationDirectoryName(version));
    }

    private String installationDirectoryName(String version) {
        return "node-v" + version;
    }

    private void logInputStream(InputStream input) throws IOException {
        Log logger = getLog();
        InputStreamReader reader = new InputStreamReader(input);
        BufferedReader br = new BufferedReader(reader);
        String line = br.readLine();
        while (line != null) {
            logger.warn(line);
            line = br.readLine();
        }
        br.close();
    }

    private void unarchive(File input, File output) throws MojoExecutionException {
        try {
            GZIPInputStream tar = new GZIPInputStream(new FileInputStream(input));
            ArchiveInputStream ais = (new ArchiveStreamFactory()).createArchiveInputStream("tar", tar);

            final TarArchiveInputStream files = (TarArchiveInputStream) ais;
            TarArchiveEntry entry = (TarArchiveEntry) files.getNextEntry();

            while (entry != null) {
                final File outputFile = new File(output, entry.getName());

                if (entry.isDirectory()) {
                    outputFile.mkdirs();

                } else if (entry.isSymbolicLink()) {
                    File target = new File(entry.getLinkName());
                    Files.createSymbolicLink(outputFile.toPath(), target.toPath());

                } else {
                    boolean executable = (entry.getMode() % 2) == 1;

                    final OutputStream fout = new FileOutputStream(outputFile);
                    IOUtils.copy(files, fout);
                    fout.close();

                    outputFile.setExecutable(executable);
                }

                entry = (TarArchiveEntry) files.getNextEntry();
            }

        } catch (Exception e) {
            throw new MojoExecutionException("Error extracting node binary tar.gz", e);
        }
    }


}
