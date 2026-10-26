package com.rutaji.qwent3.ai;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Example AI: keeps its hand, then plays random legal moves and only passes when it has nothing else. */
public final class RandomAi implements QwentAi {
    @Override
    public AiAction decide(AiView view) {
        List<AiAction> legal = view.legalActions();
        if (legal.contains(new AiAction.KeepHand())) {
            return new AiAction.KeepHand();
        }
        List<AiAction> notPass = legal.stream().filter(action -> !(action instanceof AiAction.Pass)).toList();
        List<AiAction> options = notPass.isEmpty() ? legal : notPass;
        return options.get(ThreadLocalRandom.current().nextInt(options.size()));
    }
}
