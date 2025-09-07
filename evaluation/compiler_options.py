
class compiler_options:
    record_work_per_thread = False

    def __init__(self, number_of_threads : int = 1,
                record_work_per_thread : bool = False,
                noOptimisations : bool = False,
                smallOptimisations : bool = False,
                record_thread_stalls : bool = False,
                record_queue_size : bool = False,
                experimental_memory_manager : bool = False,
                min_queue_steal = 6,
                ):
        self.record_work_per_thread = record_work_per_thread
        self.number_of_threads = number_of_threads
        self.noOptimisations = noOptimisations
        self.smallOptimisations = smallOptimisations
        self.record_thread_stalls = record_thread_stalls
        self.record_queue_size = record_queue_size
        self.experimental_memory_manager = experimental_memory_manager
        self.min_queue_steal = min_queue_steal


    def getArgumentsForCompiler(self):
        args = []
        if self.record_work_per_thread:
            args.append("--RECORD_WORK_PER_THREAD")
        args.append("-t")
        args.append(str(self.number_of_threads))
        if self.noOptimisations:
            args.append("--NoOptimise")
        if self.smallOptimisations:
            args.append("--SmallOptimise")
        if self.record_thread_stalls:
            args.append("--RECORD_THREAD_STALLS")
        if self.record_queue_size:
            args.append("--RECORD_QUEUE_SIZE")
        if self.experimental_memory_manager:
            args.append("--EXPERIMENTAL_MEMORY_MANAGER")
        args.append("--MIN_QUEUE_SIZE_TO_ACTUALLY_STEAL ")
        args.append(str(self.min_queue_steal))

        return args

    