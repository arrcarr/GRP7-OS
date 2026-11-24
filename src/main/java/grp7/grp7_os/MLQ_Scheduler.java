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
 * A higher-priority queue always preempts a lower-priority one.
 * Each queue uses its own algorithm: FCFS, SJF, SRTF or RR.
 * Each process is assigned to a specific queue and does not move between queues.
 */

public final class MLQ_Scheduler {

    public enum Algorithm {
        FCFS, SJF, SRTF, RR
    }

    // One row of user input (queueLevel 1 = highest priority queue)
    public record ProcessInput(String id, int arrivalTime, int burstTime, int queueLevel) {
    }

    // Settings for one queue (quantumTime is only used when algorithm is RR)
    public record QueueConfig(int level, Algorithm algorithm, int quantumTime) {
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

    // Everything calculate() returns: per-process results, Gantt chart, and averages
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
        private final QueueConfig config;
        private final ArrayDeque<ProcessState> ready = new ArrayDeque<>();

        private QueueState(QueueConfig config) {
            this.config = config;
        }
    }

    // Runs the whole MLQ simulation, one time unit per loop
    public ScheduleResult calculate(List<ProcessInput> processInputs, List<QueueConfig> queueConfigs) {
        validate(processInputs, queueConfigs);

        // Build the queues in priority order (level 1 first = highest priority)
        List<QueueState> queues = queueConfigs.stream()
                .sorted(Comparator.comparingInt(QueueConfig::level))
                .map(QueueState::new)
                .toList();

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

            // Highest priority queue that has someone waiting (null if all are empty)
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
                    && highestReadyQueue.config.level() < activeQueue.config.level()) {
                if (activeQueue.config.algorithm() == Algorithm.RR) {
                    activeQueue.ready.addLast(activeProcess);   // RR: goes to the back
                } else {
                    activeQueue.ready.addFirst(activeProcess);  // Others: goes back to the front
                }
                activeProcess = null;
                activeQueue = null;
                quantumCounter = 0;
            }

            QueueState currentQueue = activeProcess == null ? highestReadyQueue : activeQueue;
            if (activeProcess == null || currentQueue.config.algorithm() == Algorithm.SRTF) {
                if (activeProcess != null) {
                    currentQueue.ready.addFirst(activeProcess);
                }
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
                    activeQueue.config.level(), startTime, currentTime);

            if (activeProcess.remainingTime == 0) {
                // Process finished: save its completion time and free the CPU
                activeProcess.completionTime = currentTime;
                completedCount++;
                activeProcess = null;
                activeQueue = null;
                quantumCounter = 0;
            } else if (activeQueue.config.algorithm() == Algorithm.RR
                    && quantumCounter == activeQueue.config.quantumTime()) {
                // RR quantum used up: send the process to the back of its queue
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

    // Checks the input before simulating
    private static void validate(List<ProcessInput> processes, List<QueueConfig> queueConfigs) {
        Objects.requireNonNull(processes, "Processes are required.");
        Objects.requireNonNull(queueConfigs, "Queue configurations are required.");
        if (processes.isEmpty()) {
            throw new IllegalArgumentException("Add at least one process.");
        }
        // Only 2 or 3 queues are allowed
        if (queueConfigs.size() < 2 || queueConfigs.size() > 3) {
            throw new IllegalArgumentException("Configure 2 or 3 queues.");
        }

        // Queue levels must be 1, 2, (3) in order, and RR needs a quantum above 0
        for (int i = 0; i < queueConfigs.size(); i++) {
            QueueConfig queue = Objects.requireNonNull(queueConfigs.get(i), "Queue configuration is required.");
            if (queue.level() != i + 1) {
                throw new IllegalArgumentException("Queue levels must be numbered consecutively from 1.");
            }
            Objects.requireNonNull(queue.algorithm(), "Queue algorithm is required.");
            if (queue.algorithm() == Algorithm.RR && queue.quantumTime() <= 0) {
                throw new IllegalArgumentException("Round Robin quantum must be greater than 0.");
            }
        }

        // Process IDs must be unique (case-insensitive), times valid, and the queue must exist
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
            if (process.queueLevel() < 1 || process.queueLevel() > queueConfigs.size()) {
                throw new IllegalArgumentException("Every process must be assigned to a configured queue.");
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

    // Take the next process out of a queue based on that queue's algorithm
    private static ProcessState selectProcess(QueueState queue) {
        return switch (queue.config.algorithm()) {
            case FCFS, RR -> queue.ready.removeFirst();                                          // Front of the line
            case SJF -> removeMinimum(queue, Comparator.comparingInt(p -> p.input.burstTime())); // Shortest burst
            case SRTF -> removeMinimum(queue, Comparator.comparingInt(p -> p.remainingTime));    // Shortest remaining
        };
    }

    // Remove the smallest process by the comparator (tie: the one earliest in line)
    private static ProcessState removeMinimum(
            QueueState queue, Comparator<ProcessState> comparator) {
        ProcessState selected = queue.ready.stream().min(comparator).orElseThrow();
        queue.ready.remove(selected);
        return selected;
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