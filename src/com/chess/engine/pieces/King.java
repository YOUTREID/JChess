package com.chess.engine.pieces;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.BoardUtils;
import com.chess.engine.board.Move;
import com.chess.engine.board.Tile;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.chess.engine.board.Move.*;
import static com.chess.engine.board.Move.AttackMove;
import static com.chess.engine.board.Move.MajorMove;

public class King extends Piece {

    private static final int[] POSSIBLE_OFFSET = {-9, -8, -7, -1, 1, 7, 8, 9};

    public King(final Alliance pieceAlliance, final int piecePosition) {
        super(Type.KING, piecePosition, pieceAlliance, true);
    }

    public King(final Alliance pieceAlliance, final int piecePosition, final boolean isFirstMove) {
        super(Type.KING, piecePosition, pieceAlliance, isFirstMove);
    }

    @Override
    public Collection<Move> calculateLegalMoves(Board board) {
        final List<Move> legalMoves = new ArrayList<>();

        for (int current : POSSIBLE_OFFSET) {
            final int destination = this.piecePosition + current;
            if (isFirstColumnExclusion(this.piecePosition, current) ||
                isEighthColumnExclusion(this.piecePosition, current)) {
                continue;
            }

            if (BoardUtils.isValid(destination)) {
                final Tile candidateDestinationTile = board.getTile(destination);
                if (!candidateDestinationTile.occupied()) {
                    legalMoves.add(new MajorMove(board, this, destination));
                } else {
                    final Piece pieceAtDestination = candidateDestinationTile.getPiece();
                    final Alliance pieceAlliance = pieceAtDestination.getPieceAlliance();
                    if (this.pieceAlliance != pieceAlliance) {
                        legalMoves.add(new MajorAttackMove(board, this, destination, pieceAtDestination));
                    }
                }
            }
        }

        return legalMoves;
    }

    @Override
    public King movePiece(Move move) {
        return new King(move.getMovedPiece().getPieceAlliance(), move.getDestination());
    }

    @Override
    public String toString() {
        return Type.KING.toString();
    }

    private static boolean isFirstColumnExclusion(final int currentPosition, final int candidateOffset) {
        return BoardUtils.FIRST_COLUMN[currentPosition] && (candidateOffset == -9 || candidateOffset == -1 ||
                candidateOffset == 7);
    }

    private static boolean isEighthColumnExclusion(final int currentPosition, final int candidateOffset) {
        return BoardUtils.SECOND_COLUMN[currentPosition] && (candidateOffset == -7 || candidateOffset == 1 ||
                candidateOffset == 9);
    }

    public boolean isCastled() {
        return false;
    }
}
