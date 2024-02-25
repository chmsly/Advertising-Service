package com.amazon.ata.advertising.service.targeting;

import com.amazon.ata.advertising.service.model.RequestContext;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicate;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicateResult;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Evaluates TargetingPredicates for a given RequestContext.
 */
public class TargetingEvaluator {
    public static final boolean IMPLEMENTED_STREAMS = true;
    public static final boolean IMPLEMENTED_CONCURRENCY = true;
    private final RequestContext requestContext;
    private final ExecutorService executorService;

    /**
     * Creates an evaluator for targeting predicates.
     * @param requestContext Context that can be used to evaluate the predicates.
     */
    public TargetingEvaluator(RequestContext requestContext) {
        this.requestContext = requestContext;
        this.executorService = Executors.newFixedThreadPool(4);
    }

    /**
     * Evaluate a TargetingGroup to determine if all of its TargetingPredicates are TRUE or not for the given
     * RequestContext.
     * @param targetingGroup Targeting group for an advertisement, including TargetingPredicates.
     * @return TRUE if all the TargetingPredicates evaluate to TRUE against the RequestContext, FALSE otherwise.
     */
//    public TargetingPredicateResult evaluate(TargetingGroup targetingGroup) {
//        boolean allTruePredicates =  targetingGroup.getTargetingPredicates()
//                .stream()
//                .allMatch(targetingPredicate -> targetingPredicate.evaluate(requestContext).isTrue());
//
//        return allTruePredicates ? TargetingPredicateResult.TRUE :
//                TargetingPredicateResult.FALSE;
//    }

    public TargetingPredicateResult evaluate(TargetingGroup targetingGroup) {
        List<Callable<Boolean>> tasks = targetingGroup.getTargetingPredicates().stream()
                .map(predicate -> (Callable<Boolean>) () -> predicate.evaluate(requestContext).isTrue())
                .collect(Collectors.toList());

        try {
            List<Future<Boolean>> futures = executorService.invokeAll(tasks);

            for (Future<Boolean> future : futures) {
                // If any of the predicates is false, return FALSE
                if (!future.get()) {
                    return TargetingPredicateResult.FALSE;
                }
            }
            // All predicates returned true
            return TargetingPredicateResult.TRUE;
        } catch (InterruptedException | ExecutionException e) {
            // Handle the exception appropriately - this could be a log, rethrow, or another action
            e.printStackTrace();
            return TargetingPredicateResult.FALSE;
        }
    }

}