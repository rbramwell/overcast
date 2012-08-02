package com.xebialabs.overcast;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class VagrantCloudHost implements CloudHost {

	public static final String VAGRANT_DIR_PROPERTY_SUFFIX = ".vagrantDir";

	public static final String VAGRANT_VM_PROPERTY_SUFFIX = ".vagrantVm";
	
	public static final String VAGRANT_IP_PROPERTY_SUFFIX = ".vagrantIp";

	private String hostLabel;

	private String vagrantDir;

	private String vagrantVm;

	private String vagrantIp;

	private boolean createdBeforeSetup;

	public VagrantCloudHost(String hostLabel, String vagrantDir, String vagrantVm, String vagrantIp) {
		this.hostLabel = hostLabel;
		this.vagrantDir = vagrantDir;
		this.vagrantVm = vagrantVm;
		this.vagrantIp = vagrantIp;
	}

	@Override
	public void setup() {
		StringBuilder statusOutput = new StringBuilder();
		int statusExitCode = vagrant(statusOutput, "vagrant", "status");
		if (statusExitCode != 0) {
			throw new RuntimeException("Cannot vagrant status host " + hostLabel + ": " + statusExitCode);
		}
		createdBeforeSetup = !statusOutput.toString().contains("not created");

        if (createdBeforeSetup) {
            logger.info("Vagrant host {} has already been created. Not upping it.", hostLabel);
        } else {
            logger.info("Upping vagrant host {}", hostLabel);
            int upExitCode = vagrant("vagrant", "up");
            if (upExitCode != 0) {
                throw new RuntimeException("Cannot vagrant up host " + hostLabel + ": " + upExitCode);
            }
        }
	}

	@Override
	public void teardown() {
		if (createdBeforeSetup) {
            logger.info("Vagrant host {} had already been created before the test started. Not destroying it.", hostLabel);
		} else {
			logger.info("Destroying vagrant host {}", hostLabel);

			int exitCode = vagrant("vagrant", "destroy", "-f");
			if (exitCode != 0) {
				throw new RuntimeException("Cannot vagrant destroy host " + hostLabel + ": " + exitCode);
			}
		}
	}

	@Override
	public String getHostName() {
		return vagrantIp;
	}

	@Override
	public int getPort(int port) {
		return port;
	}

	private int vagrant(final String... command) {
		try {
			Process vagrant = startVagrant(command);
			showProcessOutput(vagrant.getInputStream(), System.out);
			showProcessOutput(vagrant.getErrorStream(), System.err);
			return vagrant.waitFor();
		} catch (IOException e) {
			throw new RuntimeException("Cannot execute " + on(" ").join(command) + " for host " + hostLabel);
		} catch (InterruptedException e) {
			throw new RuntimeException("Cannot execute " + on(" ").join(command) + " for host " + hostLabel);
		}
	}

	private int vagrant(final StringBuilder output, final String... command) {
		try {
			Process vagrant = startVagrant(command);
			gatherProcessOutput(vagrant.getInputStream(), output);
			showProcessOutput(vagrant.getErrorStream(), System.err);
			return vagrant.waitFor();
		} catch (IOException e) {
			throw new RuntimeException("Cannot execute " + on(" ").join(command) + " for host " + hostLabel);
		} catch (InterruptedException e) {
			throw new RuntimeException("Cannot execute " + on(" ").join(command) + " for host " + hostLabel);
		}
	}

	private Process startVagrant(final String... command) throws IOException {
		ProcessBuilder pb;
		if (vagrantVm == null) {
			pb = new ProcessBuilder(command);
		} else {
			List<String> commandWithVagrantVm = newArrayList(command);
			commandWithVagrantVm.add(vagrantVm);
			pb = new ProcessBuilder(commandWithVagrantVm);
		}
		pb.directory(new File(vagrantDir));
		return pb.start();
	}

	private void showProcessOutput(final InputStream from, final PrintStream to) {
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					for (;;) {
						int c = from.read();
						if (c == -1)
							break;
						to.write((char) c);
					}
				} catch (IOException ignore) {
				}
			}
		});
		t.start();
	}

	private void gatherProcessOutput(final InputStream from, final StringBuilder output) {
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					for (;;) {
						int c = from.read();
						if (c == -1)
							break;
						output.append((char) c);
					}
				} catch (IOException ignore) {
				}
			}
		});
		t.start();
	}

	private static Logger logger = LoggerFactory.getLogger(VagrantCloudHost.class);

}