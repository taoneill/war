package com.tommytony.war.jobs;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.scheduler.BukkitScheduler;

import bukkit.tommytony.war.War;

public class WarJobQueue extends Thread {
	
	private final War war;
	private Queue<Runnable> queue = new ConcurrentLinkedQueue<Runnable>();

	public WarJobQueue(War war) {
		super("War - Job Queue");  // Initialize thread.
		this.war = war;
		start();
	}
	
	public void run() {
		war.logInfo(Thread.currentThread().getName() + " running...");
		while(!isInterrupted()) {
			if(!queue.isEmpty()) {
				Runnable job = queue.poll();
				runJob(job);
			}
		}
		war.logInfo("War - Job Queue interrupted.");
	}
	
	public synchronized void addJob(Runnable job) {
		queue.add(job);
	}
	
	private void runJob(Runnable job) {
		BukkitScheduler scheduler = war.getServer().getScheduler();
		int taskId = scheduler.scheduleAsyncDelayedTask(war, job);
		while(scheduler.isQueued(taskId) || scheduler.isCurrentlyRunning(taskId)) {
			try {
				sleep(20);
			} catch (InterruptedException e) {
				war.logWarn("Interrupted while waiting for job to complete (" + taskId + ")");
				e.printStackTrace();
			}
		}
	}
}
