package grp7.grp7_os;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Author: Prince Irwin Martin C. Diaz
 * Algorithm: Multi-level Queue Scheduling
 * Conventions:
 * Queue 1 and queue 2 use Round Robin; queue 3 uses FCFS.
 * A higher-priority queue always preempts a lower-priority one.
 * Each process is assigned to a specific queue and does not move between queues.
 */

public final class MLQ_Scheduler {

    // One row of user input (queueLevel 1 = highest priority queue)
    public record ProcessInput(String id, int arrivalTime, int burstTime, int queueLevel) {
    }

    // Final computed values for one process (used for the results table)
    public record ProcessResult(
            String id,
            int queueLevel,
            int arrivalTime,
            int burstTime,
            int completionTime,
            int turnaroundTime,
            int waitingTime) {
    }

    // One Gantt chart block
    public record GanttBlock(String processId, int queueLevel, int startTime, int endTime) {
        public boolean isIdle() {
            return queueLevel == 0;
        }

        public String label() {
            return isIdle() ? "IDLE" : processId + " (Q" + queueLevel + ")";
        }
    }

    // Everything calculate() returns
    public record ScheduleResult(
            List<ProcessResult> processes,
            List<GanttBlock> ganttBlocks,
            double averageTurnaroundTime,
            double averageWaitingTime) {
    }

    private static final class ProcessState {
        private final ProcessInput input;
        private final int inputOrder;
        private int remainingTime;
        private int completionTime;

        private ProcessState(ProcessInput input, int inputOrder) {
            this.input = input;
            this.inputOrder = inputOrder;
            this.remainingTime = input.burstTime();
        }
    }

    // A queue's settings plus its ready line (processes that arrived and are waiting)
    private static final class QueueState {
        private final int level;
        private final int quantumTime;
        private final ArrayDeque<ProcessState> ready = new ArrayDeque<>();

        private QueueState(int level, int quantumTime) {
            this.level = level;
            this.quantumTime = quantumTime;
        }

        private boolean usesRoundRobin() {
            return level < 3;
        }
    }

    // Runs the whole MLQ simulation, one time per loop
    public ScheduleResult calculate(
            List<ProcessInput> processInputs, int queue1Quantum, int queue2Quantum) {
        validate(processInputs, queue1Quantum, queue2Quantum);

        // Queues 1 and 2 use RR, queue 3 uses FCFS
        List<QueueState> queues = List.of(
                new QueueState(1, queue1Quantum),
                new QueueState(2, queue2Quantum),
                new QueueState(3, 0));

        List<ProcessState> processes = new ArrayList<>();
        for (int i = 0; i < processInputs.size(); i++) {
            processes.add(new ProcessState(processInputs.get(i), i));
        }

        // Sort by arrival time (tie: input order) so we can admit them one by one
        List<ProcessState> arrivals = processes.stream()
                .sorted(Comparator.comparingInt((ProcessState p) -> p.input.arrivalTime())
                        .thenComparingInt(p -> p.inputOrder))
                .toList();

        List<GanttBlock> ganttBlocks = new ArrayList<>();
        int nextArrival = 0;
        int currentTime = 0;
        int completedCount = 0;
        int quantumCounter = 0;
        ProcessState activeProcess = null;
        QueueState activeQueue = null;

        // Loop until all processes are finished
        while (completedCount < processes.size()) {

            // Move every process that has arrived into the ready line of its own queue
            while (nextArrival < arrivals.size()
                    && arrivals.get(nextArrival).input.arrivalTime() <= currentTime) {
                ProcessState arrived = arrivals.get(nextArrival++);
                queues.get(arrived.input.queueLevel() - 1).ready.addLast(arrived);
            }

            // Highest priority queue that has someone waiting
            QueueState highestReadyQueue = highestNonEmptyQueue(queues);

            // Nothing running and nothing waiting
            if (activeProcess == null && highestReadyQueue == null) {
                int nextTime = arrivals.get(nextArrival).input.arrivalTime();
                addGanttBlock(ganttBlocks, null, 0, currentTime, nextTime);
                currentTime = nextTime;
                continue;
            }

            // Preemption: a higher queue now has a waiting process, so kick the running one off
            if (activeProcess != null && highestReadyQueue != null
                    && highestReadyQueue.level < activeQueue.level) {
                if (activeQueue.usesRoundRobin()) {
                    activeQueue.ready.addLast(activeProcess);   // RR: goes to the back
                } else {
                    activeQueue.ready.addFirst(activeProcess);  // Others: goes back to the front
                }
                activeProcess = null;
                activeQueue = null;
                quantumCounter = 0;
            }

            QueueState currentQueue = activeProcess == null ? highestReadyQueue : activeQueue;
            if (activeProcess == null) {
                activeProcess = selectProcess(currentQueue);
                activeQueue = currentQueue;
                quantumCounter = 0;
            }

            // Run the chosen process for exactly 1 time unit
            int startTime = currentTime;
            currentTime = Math.addExact(currentTime, 1);
            activeProcess.remainingTime--;
            quantumCounter++;
            addGanttBlock(ganttBlocks, activeProcess.input.id(),
                    activeQueue.level, startTime, currentTime);

            if (activeProcess.remainingTime == 0) {
                // Process finished
                activeProcess.completionTime = currentTime;
                completedCount++;
                activeProcess = null;
                activeQueue = null;
                quantumCounter = 0;
            } else if (activeQueue.usesRoundRobin()
                    && quantumCounter == activeQueue.quantumTime) {
                // RR quantum used up. Send the process to the back of its queue
                activeQueue.ready.addLast(activeProcess);
                activeProcess = null;
                activeQueue = null;
                quantumCounter = 0;
            }
        }

        List<ProcessResult> results = new ArrayList<>();
        double totalTurnaround = 0;
        double totalWaiting = 0;
        for (ProcessState process : processes) {
            int turnaround = process.completionTime - process.input.arrivalTime(); // TAT = CT - AT
            int waiting = turnaround - process.input.burstTime();                  // WT = TAT - BT
            results.add(new ProcessResult(
                    process.input.id(),
                    process.input.queueLevel(),
                    process.input.arrivalTime(),
                    process.input.burstTime(),
                    process.completionTime,
                    turnaround,
                    waiting));
            totalTurnaround += turnaround;
            totalWaiting += waiting;
        }

        // Compute final averages
        return new ScheduleResult(
                List.copyOf(results),
                List.copyOf(ganttBlocks),
                totalTurnaround / processes.size(),
                totalWaiting / processes.size());
    }

    // Checks the input
    private static void validate(
            List<ProcessInput> processes, int queue1Quantum, int queue2Quantum) {
        Objects.requireNonNull(processes, "Processes are required.");
        if (processes.isEmpty()) {
            throw new IllegalArgumentException("Add at least one process.");
        }
        if (queue1Quantum <= 0 || queue2Quantum <= 0) {
            throw new IllegalArgumentException("Round Robin quantum must be greater than 0.");
        }

        // Process IDs must be unique
        Set<String> ids = new HashSet<>();
        for (ProcessInput process : processes) {
            Objects.requireNonNull(process, "Process details are required.");
            if (process.id() == null || process.id().isBlank()) {
                throw new IllegalArgumentException("Process ID cannot be empty.");
            }
            if (!ids.add(process.id().trim().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Process IDs must be unique.");
            }
            if (process.arrivalTime() < 0 || process.burstTime() <= 0) {
                throw new IllegalArgumentException("Arrival time must be >= 0 and burst time must be > 0.");
            }
            if (process.queueLevel() < 1 || process.queueLevel() > 3) {
                throw new IllegalArgumentException("Every process must be assigned to queue 1, 2, or 3.");
            }
        }
    }

    private static QueueState highestNonEmptyQueue(List<QueueState> queues) {
        for (QueueState queue : queues) {
            if (!queue.ready.isEmpty()) {
                return queue;
            }
        }
        return null;
    }

    private static ProcessState selectProcess(QueueState queue) {
        return queue.ready.removeFirst();
    }

    private static void addGanttBlock(
            List<GanttBlock> blocks, String processId, int queueLevel, int startTime, int endTime) {
        if (!blocks.isEmpty()) {
            GanttBlock last = blocks.get(blocks.size() - 1);
            if (last.queueLevel() == queueLevel
                    && Objects.equals(last.processId(), processId)
                    && last.endTime() == startTime) {
                blocks.set(blocks.size() - 1,
                        new GanttBlock(processId, queueLevel, last.startTime(), endTime));
                return;
            }
        }
        blocks.add(new GanttBlock(processId, queueLevel, startTime, endTime));
    }
}