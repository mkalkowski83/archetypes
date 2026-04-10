package com.softwarearchetypes.graphs.userjourney;

import static java.util.stream.Collectors.toSet;

import com.softwarearchetypes.graphs.userjourney.Product.ProductType;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultDirectedGraph;

record UserJourney(UserJourneyId userJourneyId, Graph<State, Condition> graph, State currentState) {

    static Builder builder(UserJourneyId userJourneyId) {
        return new Builder(userJourneyId);
    }

    Set<CustomerPath> waysToAchieve(ProductType productType) {
        Set<State> statesWithProduct =
                graph.vertexSet().stream()
                        .filter(state -> state.contains(productType))
                        .collect(toSet());
        return statesWithProduct.stream()
                .flatMap(
                        targetState ->
                                new AllDirectedPaths<>(graph)
                                                .getAllPaths(
                                                        currentState(), targetState, true, null)
                                                .stream())
                .map(graphPath -> CustomerPath.of(graphPath.getEdgeList()))
                .collect(toSet());
    }

    UserJourney onFulfilled(Condition condition) {
        Set<Condition> outgoingEdges = graph.outgoingEdgesOf(currentState);
        return outgoingEdges.stream()
                .filter(edge -> edge.equals(condition))
                .map(graph::getEdgeTarget)
                .findFirst()
                .map(targetState -> new UserJourney(userJourneyId, graph, targetState))
                .orElse(this);
    }

    Optional<CustomerPath> optimizedWayToAchieve(
            ProductType productType, Function<Condition, Double> weightFunction) {
        return waysToAchieve(productType).stream()
                .min(Comparator.comparingDouble(path -> path.weight(weightFunction)));
    }
}

class Builder {
    final UserJourneyId userJourneyId;
    final Graph<State, Condition> graph;
    State currentState;

    Builder(UserJourneyId userJourneyId) {
        this.userJourneyId = userJourneyId;
        this.graph = new DefaultDirectedGraph<>(Condition.class);
    }

    TransitionBuilder from(State state) {
        graph.addVertex(state);
        return new TransitionBuilder(this, state);
    }

    Builder withCurrentState(State currentState) {
        this.currentState = currentState;
        return this;
    }

    UserJourney build() {
        return new UserJourney(userJourneyId, graph, currentState);
    }
}

class TransitionBuilder {
    private final Builder builder;
    private final State fromState;

    TransitionBuilder(Builder builder, State fromState) {
        this.builder = builder;
        this.fromState = fromState;
    }

    TransitionWithCondition on(Condition condition) {
        return new TransitionWithCondition(builder, fromState, condition);
    }
}

class TransitionWithCondition {
    private final Builder builder;
    private final State fromState;
    private final Condition condition;

    TransitionWithCondition(Builder builder, State fromState, Condition condition) {
        this.builder = builder;
        this.fromState = fromState;
        this.condition = condition;
    }

    Builder goto_(State toState) {
        builder.graph.addVertex(toState);
        builder.graph.addEdge(fromState, toState, condition);
        return builder;
    }
}
