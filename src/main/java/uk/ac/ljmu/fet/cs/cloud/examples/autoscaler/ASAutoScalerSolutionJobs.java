package uk.ac.ljmu.fet.cs.cloud.examples.autoscaler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.Job;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.IaaSService;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VirtualMachine;

/**
 * This autoscaler takes in the amount of jobs and ensures there are a specific
 * amount of VMs per type of job in proportion to the amount of each job type
 * and the specified amount of VMs to be created in the CLI arguments
 */
public class ASAutoScalerSolutionJobs extends VirtualInfrastructure {

	/**
	 * Initialises the auto scaling mechanism
	 * 
	 * @param cloud
	 *            the physical infrastructure to use to rent the VMs from
	 */
	public ASAutoScalerSolutionJobs(final IaaSService cloud) {
		super(cloud);
	}

	// Hashmap to store the number of jobs for each type of job
	static HashMap<String, Integer> jobTypeInfoTable = new HashMap<String, Integer>();
	// Hashmap to store the calcualted proportions when considering how many VMs
	// have been requested
	static HashMap<String, Integer> actualProportionsOfVMs = new HashMap<String, Integer>();
	static int numberOfTotalRequestedPMs;

	/**
	 * This class will take the input of the total number of VMs requested in
	 * the CLI arguments and will calculate the required information to
	 * proportionally scale the VI
	 */

	public static void evaluateJobs(int numOfPMs) {

		numberOfTotalRequestedPMs = numOfPMs;
	

		List<Job> getAllJobs = JobArrivalHandler.getJobs();
		double totalJobs = getAllJobs.size();
		String currName = "";
		double currNum;
		double percent;

		// For loop to identify each unique job type and count the totals
		for (int i = 0; i < totalJobs; i++) {

			currName = getAllJobs.get(i).executable;
			if (jobTypeInfoTable.containsKey(currName)) {

				jobTypeInfoTable.put(currName, jobTypeInfoTable.get(currName) + 1);

			} else {
				jobTypeInfoTable.put(currName, 1);
			}

		}

		System.out.println("Number of Requested PMs = " + numberOfTotalRequestedPMs);
		System.out.println("The total types of Jobs and their amount are: " + jobTypeInfoTable);
		System.out.println("The breakdown of job type is: ");

		// for loop to calculate the proportions
		for (String i : jobTypeInfoTable.keySet()) {

			currNum = jobTypeInfoTable.get(i);
			percent = Math.round((currNum / totalJobs) * 100);

			int NewPercent = (int) percent;

			System.out.println("percent of " + i + " Type = " + NewPercent + "%");

			Double b = Double.valueOf(numberOfTotalRequestedPMs);
			double a = (b / 100) * percent;
			int c = (int) Math.round(a);
			actualProportionsOfVMs.put(i, c);
			System.out.println(
					"This equates to " + c + " Virtual Machines in proportion to The amount of Physical Machines");

		}

	}

	@Override

	/**
	 * This class will ensure that there are only the calculated amount of VMs
	 * to proportionally represent the incoming jobs it does this by adding and
	 * destroying VM if required
	 */

	public void tick(long fires) {

		final Iterator<String> kinds = vmSetPerKind.keySet().iterator();
		
		while (kinds.hasNext()) {

			final String kind = kinds.next();
			final ArrayList<VirtualMachine> vmset = vmSetPerKind.get(kind);
			int currentVMSize = vmset.size();
			int currentProportion = actualProportionsOfVMs.get(kind);

			if (currentVMSize < currentProportion) {

				requestVM(kind);
				System.out.println(
						"Adding a VM to " + kind + " as currently " + currentVMSize + " not " + currentProportion);

			} else if (currentVMSize > currentProportion) {

				System.out.println(
						"Removing a VM from " + kind + " as currently " + currentVMSize + " not " + currentProportion);
				destroyVM(vmset.get(0));
			} else if (currentVMSize == currentProportion) {

				// Do nothing as size successfully scaled
			}
		}

	}
}
