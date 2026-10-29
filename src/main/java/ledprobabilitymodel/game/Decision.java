package ledprobabilitymodel.game;

/** A claim size of zero denotes a terminal game; reveal decisions also use zero. */
public record Decision(Action action, int claimSize) {}
