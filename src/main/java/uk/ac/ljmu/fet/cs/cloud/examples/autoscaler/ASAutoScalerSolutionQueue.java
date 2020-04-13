package uk.ac.ljmu.fet.cs.cloud.examples.autoscaler;

import java.util.ArrayList;
import java.util.HashMap;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.IaaSService;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VirtualMachine;

/**
 * This autoscaler takes in the amount of jobs in the Queue and ensures there are a specific
 * amount of VMs per type of job in proportion to the amount of each job type in the Queue
 * and the specified amount of PMs to be created in the CLI arguments
 */
public class ASAutoScalerSolutionQueue extends VirtualInfrastructure {

	/**
	 * Initialises the auto scaling mechanism
	 * 
	 * @param cloud
	 *            the physical infrastructure to use to rent the VMs from
	 */
	public ASAutoScalerSolutionQueue(final IaaSService cloud) {
		super(cloud);
	}

	// Hashmap to store the number of jobs for each type of job
	static HashMap<String, Integer> queueJobTypeInfoTable = new HashMap<String, Integer>();

	// Hashmap to store the calcualted proportions when considering how many VMs
	// have been requested
	static HashMap<String, Integer> actualProportionsOfVMs = new HashMap<String, Integer>();

	static int numberOfTotalRequestedPMs;
	static boolean queueEmpty = true;

	/**
	 * This class will take the input of the total number of PMs requested in
	 * the CLI arguments and will calculate the required information to
	 * proportionally scale the VI
	 */

	public static void evaluateJobs(int numOfPMs) {
		
		numberOfTotalRequestedPMs = numOfPMs;
		QueueManager test = AutoScalingDemo.getQueue();
		int totalJobsInQueue = 0;

		// If the queue is not empty 
		if (!test.getQueue().keySet().isEmpty()) {
			//reset the hashmap as jobs may be finished
			
			queueJobTypeInfoTable.clear();

			for (String i : test.getQueue().keySet()) {
				
				System.out.println("Amount of " + i + "Type in the Queue: " + test.getQueue().get(i).size());

				queueJobTypeInfoTable.put(i, +test.getQueue().get(i).size());
				totalJobsInQueue = totalJobsInQueue + test.getQueue().get(i).size();
				System.out.println("The Total number of jobs in queue are: " + totalJobsInQueue);
			}

			System.out.println("The current jobs in queue are: " + queueJobTypeInfoTable);

			// for loop to calculate the proportions
			actualProportionsOfVMs.clear();
			for (String i : queueJobTypeInfoTable.keySet()) {

				double currNum = queueJobTypeInfoTable.get(i);
				double percent = Math.round((currNum / totalJobsInQueue) * 100);

				
				System.out.println("percent of " + i + " Type = " + (int) percent + "%");

				Double b = Double.valueOf(numberOfTotalRequestedPMs);
				double a = (b / 100) * percent;
				int c = (int) Math.round(a);
				
				// ensuring that there is always at least 1 VM for each type of Job
				if (c < 1){
					c=1;
				}
				
				actualProportionsOfVMs.put(i, c);
				System.out.println(
						"This equates to " + c + " Virtual Machines in proportion to The amount of Physical Machines" + "(" + numberOfTotalRequestedPMs  + ")");

			}

		} else {

			queueEmpty = false;
			System.out.println(queueEmpty);
		}

	}

	@Override

	/**
	 * This class will ensure that there are only the calculated amount of VMs
	 * to proportionally represent the Queued jobs it does this by adding and
	 * destroying VM if required
	 */

	public void tick(long fires) {

		if(queueEmpty = false){
			evaluateJobs(numberOfTotalRequestedPMs);
		}
		

		for (String i : queueJobTypeInfoTable.keySet()) {

			final ArrayList<VirtualMachine> vmset = vmSetPerKind.get(i);
			int currentVMSize = vmset.size();
			int currentProportion = actualProportionsOfVMs.get(i);

			if (currentVMSize < currentProportion) {

				requestVM(i);
				System.out.println("Adding a VM to " + i + " as currently " + currentVMSize + " not " + currentProportion);

			} else if (currentVMSize > currentProportion) {

				System.out.println("Removing a VM from " + i + " as currently " + currentVMSize + " not " + currentProportion);
				destroyVM(vmset.get(0));
			} else if (currentVMSize == currentProportion) {

				// Do nothing as size successfully scaled
			}
			
		}
	
		
		
	}
}
