package com.chess.engine.pieces;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.BoardUtils;
import com.chess.engine.board.Move;
import com.chess.engine.board.Move.MajorMove;
import com.chess.engine.board.Tile;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.chess.engine.board.Move.*;

public class Bishop extends Piece {

    private final static  int[] POSSIBLE_OFFSETS = {-9, -7, 7, 9};

    Bishop(int piecePosition, Alliance pieceAlliance) {
        super(piecePosition, pieceAlliance);
    }

    @Override
    public Collection<Move> calculateLegalMoves(final Board board) {
        final List<Move> legalMoves = new ArrayList<>();
        for (final int candidateOffset : POSSIBLE_OFFSETS) {
            int destination = this.piecePosition;
            while (BoardUtils.isValid(destination)) {
                if (isFirstColumnExclusion(destination, candidateOffset) ||
                isEigthColumnExclusion(destination, candidateOffset)) {
                    break;
                }
                destination += candidateOffset;
                if (BoardUtils.isValid((destination))) {
                    final Tile candidateDestinationTile = board.getTile(destination);
                    if (!candidateDestinationTile.occupied()) {
                        legalMoves.add(new MajorMove(board, this, destination));
                    } else {
                        final Piece pieceAtDestination = candidateDestinationTile.getPiece();
                        final Alliance pieceAlliance = pieceAtDestination.getPieceAlliance();
                        if (this.pieceAlliance != pieceAlliance) {
                            legalMoves.add(new AttackMove(board, this, destination, pieceAtDestination));
                        }
                    }
                    break;
                }
            }
        }
        return ImmutableList.copyOf(legalMoves);
    }

    private static boolean isFirstColumnExclusion(final int current, final int candidateOffset) {
        return BoardUtils.FIRST_COLUMN[current] && (candidateOffset == -9 || candidateOffset == 7);
    }

    private static boolean isEigthColumnExclusion(final int current, final int candidateOffset) {
        return BoardUtils.EIGHTH_COLUMN[current] && (candidateOffset == -7 || candidateOffset == 9);
    }
}