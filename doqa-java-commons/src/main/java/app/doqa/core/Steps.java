package app.doqa.core;

/**
 * Step-stack mechanics shared by the {@code Doqa} facade, the AspectJ aspect and framework
 * extensions: a pushed step nests under the currently open one (or the phase bucket when the
 * stack is empty).
 *
 * <p>Internal adapter API - user code opens steps via {@code Doqa.step}.
 */
public final class Steps {

    private Steps() {
    }

    public static StepNode push(String title) {
        return push(title, null);
    }

    public static StepNode push(String title, String description) {
        RuntimeContext ctx = DoqaContexts.current();
        if (ctx == null) {
            return null;
        }
        StepNode node = new StepNode(title);
        node.description = description;
        if (!ctx.stepStack.isEmpty()) {
            ctx.stepStack.peek().children.add(node);
        } else {
            ctx.phaseBucket().add(node);
        }
        ctx.stepStack.push(node);
        return node;
    }

    public static void pop(String outcome, String message) {
        RuntimeContext ctx = DoqaContexts.current();
        if (ctx == null || ctx.stepStack.isEmpty()) {
            return;
        }
        StepNode node = ctx.stepStack.pop();
        node.durationMs = System.currentTimeMillis() - node.startMillis;
        node.outcome = outcome;
        if (message != null) {
            // never lose the failure cause: appended after any user addMessage note
            node.message = node.message == null ? message : node.message + "\n" + message;
        }
    }
}
