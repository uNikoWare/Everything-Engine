package com.everythingengine.core.spatial;

import java.util.Objects;

/**
 * Rigid transform from one coordinate space to another.
 * Rotation is a right-handed orthonormal 3x3 matrix; translation is in parsecs.
 */
public final class CoordinateTransform {
    private static final double TOLERANCE = 1.0e-10;

    private final CoordinateSpace source;
    private final CoordinateSpace target;
    private final double[][] rotation;
    private final Vector3D translation;

    public CoordinateTransform(CoordinateSpace source, CoordinateSpace target,
            double[][] rotation, Vector3D translation) {
        this.source = Objects.requireNonNull(source, "source");
        this.target = Objects.requireNonNull(target, "target");
        this.rotation = copyAndValidate(rotation);
        this.translation = Objects.requireNonNull(translation, "translation");
    }

    public static CoordinateTransform translation(CoordinateSpace source, CoordinateSpace target,
            Vector3D translation) {
        return new CoordinateTransform(source, target,
                new double[][] {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}}, translation);
    }

    public Position3D transform(Position3D position) {
        Objects.requireNonNull(position, "position");
        if (!source.equals(position.space())) {
            throw new IllegalArgumentException("Position is not in the transform's source space");
        }
        Vector3D rotated = rotate(new Vector3D(position.x(), position.y(), position.z()));
        return new Position3D(target,
                rotated.x() + translation.x(),
                rotated.y() + translation.y(),
                rotated.z() + translation.z());
    }

    public Direction3D transform(Direction3D direction) {
        Objects.requireNonNull(direction, "direction");
        return Direction3D.from(rotate(direction.asVector()));
    }

    public CoordinateTransform inverse() {
        double[][] inverseRotation = transpose(rotation);
        Vector3D inverseTranslation = rotate(inverseRotation, translation).scale(-1);
        return new CoordinateTransform(target, source, inverseRotation, inverseTranslation);
    }

    public CoordinateSpace source() {
        return source;
    }

    public CoordinateSpace target() {
        return target;
    }

    private Vector3D rotate(Vector3D vector) {
        return rotate(rotation, vector);
    }

    private static Vector3D rotate(double[][] matrix, Vector3D vector) {
        return new Vector3D(
                matrix[0][0] * vector.x() + matrix[0][1] * vector.y() + matrix[0][2] * vector.z(),
                matrix[1][0] * vector.x() + matrix[1][1] * vector.y() + matrix[1][2] * vector.z(),
                matrix[2][0] * vector.x() + matrix[2][1] * vector.y() + matrix[2][2] * vector.z());
    }

    private static double[][] transpose(double[][] matrix) {
        double[][] result = new double[3][3];
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                result[row][column] = matrix[column][row];
            }
        }
        return result;
    }

    private static double[][] copyAndValidate(double[][] matrix) {
        Objects.requireNonNull(matrix, "rotation");
        if (matrix.length != 3) {
            throw new IllegalArgumentException("Rotation must be a 3x3 matrix");
        }
        double[][] copy = new double[3][3];
        for (int row = 0; row < 3; row++) {
            if (matrix[row] == null || matrix[row].length != 3) {
                throw new IllegalArgumentException("Rotation must be a 3x3 matrix");
            }
            for (int column = 0; column < 3; column++) {
                Vector3D.requireFinite(matrix[row][column], "rotation element");
                copy[row][column] = matrix[row][column];
            }
        }
        for (int first = 0; first < 3; first++) {
            for (int second = 0; second < 3; second++) {
                double dot = 0;
                for (int row = 0; row < 3; row++) {
                    dot += copy[row][first] * copy[row][second];
                }
                double expected = first == second ? 1.0 : 0.0;
                if (Math.abs(dot - expected) > TOLERANCE) {
                    throw new IllegalArgumentException("Rotation matrix must be orthonormal");
                }
            }
        }
        double determinant = copy[0][0] * (copy[1][1] * copy[2][2] - copy[1][2] * copy[2][1])
                - copy[0][1] * (copy[1][0] * copy[2][2] - copy[1][2] * copy[2][0])
                + copy[0][2] * (copy[1][0] * copy[2][1] - copy[1][1] * copy[2][0]);
        if (Math.abs(determinant - 1.0) > TOLERANCE) {
            throw new IllegalArgumentException("Rotation matrix must preserve right-handed orientation");
        }
        return copy;
    }
}
