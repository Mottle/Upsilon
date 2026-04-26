package com.github.wintersteve25.tau.build;

/**
 * Implemented by components that keep active mount state.
 *
 * @param <S> mount state type
 */
public interface MountStateHost<S extends MountState> {
    S getActiveMountState();

    void setActiveMountState(S state);
}
