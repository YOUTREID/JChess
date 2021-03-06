package com.chess.engine.player.ai;

import java.util.Collection;
import java.util.Comparator;
import java.util.Observable;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.BoardUtils;
import com.chess.engine.board.Move;
import com.chess.engine.board.Move.NullMove;
import com.chess.engine.player.MoveTransition;
import com.chess.engine.player.Player;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

public class AlphaBeta extends Observable implements MoveStrategy {

    private final BoardEvaluator evaluator;
    private final int depth;
    private final MoveSorter moveSorter;
    private final int quiescenceFactor;
    private long boardsEvaluated;
    private int quiescenceCount;
    private int cutOffsProduced;
    private static final int MAX_QUIESCENCE = 25000;

    private enum MoveSorter {

        SORT {
            @Override
            Collection<Move> sort(final Collection<Move> moves) {
                return Ordering.from(SMART_SORT).immutableSortedCopy(moves);
            }
        };

        public static final Comparator<Move> SMART_SORT = (move1, move2) -> ComparisonChain.start()
                .compareTrueFirst(BoardUtils.isThreatenedBoardImmediate(move1.getBoard()), BoardUtils.isThreatenedBoardImmediate(move2.getBoard()))
                .compareTrueFirst(move1.isAttack(), move2.isAttack())
                .compareTrueFirst(move1.isCastlingMove(), move2.isCastlingMove())
                .compare(move2.getMovedPiece().getPieceValue(), move1.getMovedPiece().getPieceValue())
                .result();

        abstract Collection<Move> sort(Collection<Move> moves);
    }

    public AlphaBeta(final int depth, final int quiescenceFactor) {
        this.depth = depth;
        this.evaluator = new StandardBoardEvaluator();
        this.quiescenceFactor = quiescenceFactor;
        this.moveSorter = MoveSorter.SORT;
        this.boardsEvaluated = 0;
        this.quiescenceCount = 0;
        this.cutOffsProduced = 0;
    }

    @Override
    public String toString() {
        return "AB+MO";
    }

    @Override
    public long getNumBoardsEvaluated() {
        return this.boardsEvaluated;
    }

    @Override
    public Move execute(final Board board) {
        final long startTime = System.currentTimeMillis();
        final Player currentPlayer = board.currentPlayer();
        final Alliance alliance = currentPlayer.getAlliance();
        Move bestMove = new NullMove();
        int highestSeenValue = Integer.MIN_VALUE;
        int lowestSeenValue = Integer.MAX_VALUE;
        int currentValue;
        int moveCounter = 1;
        final int numMoves = this.moveSorter.sort(board.currentPlayer().getLegalMoves()).size();
        System.out.println(board.currentPlayer() + " thinking with depth = " + depth + " (pruning enabled)");
        System.out.println("\tOrdered moves! : " + this.moveSorter.sort(board.currentPlayer().getLegalMoves()));
        for (final Move move : this.moveSorter.sort(board.currentPlayer().getLegalMoves())) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            this.quiescenceCount = 0;
            final String s;
            if (moveTransition.getMoveStatus().isDone()) {
                final long candidateMoveStartTime = System.nanoTime();
                currentValue = alliance.isWhite() ?
                        min(moveTransition.getToBoard(), depth - 1, highestSeenValue, lowestSeenValue) :
                        max(moveTransition.getToBoard(), depth - 1, highestSeenValue, lowestSeenValue);
                if (alliance.isWhite() && currentValue > highestSeenValue) {
                    highestSeenValue = currentValue;
                    bestMove = move;
                    if(moveTransition.getToBoard().blackPlayer().isInCheckMate()) {
                        break;
                    }
                }
                else if (alliance.isBlack() && currentValue < lowestSeenValue) {
                    lowestSeenValue = currentValue;
                    bestMove = move;
                    if(moveTransition.getToBoard().whitePlayer().isInCheckMate()) {
                        break;
                    }
                }
                final String quiescenceInfo = String.format(" [high: %d low: %d] quiescenceCount: %d", highestSeenValue, lowestSeenValue, this.quiescenceCount);
                s = "\t" + toString() + "(" +depth+ "), move: (" +moveCounter+ "/" +numMoves+ ") " + move + ", best: " + bestMove
                        + quiescenceInfo + ", time: " +calculateTimeTaken(candidateMoveStartTime, System.nanoTime());
            } else {
                s = "\t" + toString() + ", m: (" +moveCounter+ "/" +numMoves+ ") " + move + " is illegal, best: " +bestMove;
            }
            setChanged();
            notifyObservers(s);
            moveCounter++;
        }
        long executionTime = System.currentTimeMillis() - startTime;
        System.out.printf("%s SELECTS %s [#boards evaluated = %d, time taken = %d ms, eval rate = %.1f, cutoffCount = %d, prune percent = %.2f%%.\n", board.currentPlayer(),
                bestMove, this.boardsEvaluated, executionTime, (1000 * (double)this.boardsEvaluated/ executionTime), this.cutOffsProduced, 100 * ((double)this.cutOffsProduced/this.boardsEvaluated));
        return bestMove;
    }

    private int max(final Board board,
                    final int depth,
                    final int highest,
                    final int lowest) {
        if (depth == 0 || BoardUtils.isEndGame(board)) {
            this.boardsEvaluated++;
            return this.evaluator.evaluate(board, depth);
        }
        int currentHighest = highest;
        for (final Move move : this.moveSorter.sort((board.currentPlayer().getLegalMoves()))) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            if (moveTransition.getMoveStatus().isDone()) {
                currentHighest = Math.max(currentHighest, min(moveTransition.getToBoard(),
                        calculateQuiescenceDepth(board, depth), currentHighest, lowest));
                if (lowest <= currentHighest) {
                    this.cutOffsProduced++;
                    break;
                }
            }
        }
        return currentHighest;
    }

    private int min(final Board board,
                    final int depth,
                    final int highest,
                    final int lowest) {
        if (depth == 0 || BoardUtils.isEndGame(board)) {
            this.boardsEvaluated++;
            return this.evaluator.evaluate(board, depth);
        }
        int currentLowest = lowest;
        for (final Move move : this.moveSorter.sort((board.currentPlayer().getLegalMoves()))) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            if (moveTransition.getMoveStatus().isDone()) {
                currentLowest = Math.min(currentLowest, max(moveTransition.getToBoard(),
                        calculateQuiescenceDepth(board, depth), highest, currentLowest));
                if (currentLowest <= highest) {
                    this.cutOffsProduced++;
                    break;
                }
            }
        }
        return currentLowest;
    }

    private int calculateQuiescenceDepth(final Board board,
                                         final int depth) {
        if(depth == 1 && this.quiescenceCount < MAX_QUIESCENCE) {
            int activityMeasure = 0;
            if (board.currentPlayer().isInCheck()) {
                activityMeasure += 1;
            }
            for(final Move move: BoardUtils.lastNMoves(board, 2)) {
                if(move.isAttack()) {
                    activityMeasure += 1;
                }
            }
            if(activityMeasure >= 2) {
                this.quiescenceCount++;
                return 2;
            }
        }
        return depth - 1;
    }

    private static String calculateTimeTaken(final long start, final long end) {
        final long timeTaken = (end - start) / 1000000;
        return timeTaken + " ms";
    }


}