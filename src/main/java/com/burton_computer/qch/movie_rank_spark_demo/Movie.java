///###////////////////////////////////////////////////////////////////////////
//
// Burton Computer Corporation
// http://www.burton-computer.com
//
// Copyright (c) 2018, Burton Computer Corporation
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
//     Redistributions of source code must retain the above copyright
//     notice, this list of conditions and the following disclaimer.
//
//     Redistributions in binary form must reproduce the above copyright
//     notice, this list of conditions and the following disclaimer in
//     the documentation and/or other materials provided with the
//     distribution.
//
//     Neither the name of the Burton Computer Corporation nor the names
//     of its contributors may be used to endorse or promote products
//     derived from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.burton_computer.qch.movie_rank_spark_demo;

import org.javimmutable.collections.IterableStreamable;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.util.JImmutables;

import javax.annotation.concurrent.Immutable;
import java.io.Serializable;
import java.util.Objects;

@Immutable
public class Movie
    implements Serializable
{
    public static final int NUM_FEATURES = 5;
    public static final int BASE_RANK_SCORE = 15;

    private final int id;
    private final JImmutableMap<Feature, Integer> features;

    public Movie(int id,
                 Iterable<Feature> featuresList)
    {
        this.id = id;
        JImmutableMap<Feature, Integer> features = JImmutables.map();
        int rank = 1;
        for (Feature feature : featuresList) {
            features = features.assign(feature, rank);
            rank += 1;
        }
        this.features = features;
        assert features.size() == NUM_FEATURES;
    }

    public int getId()
    {
        return id;
    }

    public IterableStreamable<Feature> getFeatures()
    {
        return features.keys();
    }

    public int rankOf(Feature f)
    {
        return features.getValueOr(f, 0);
    }

    public MovieScore computeScoreFor(Movie other)
    {
        int score = 0;
        for (JImmutableMap.Entry<Feature, Integer> e : features) {
            final Feature feature = e.getKey();
            final int otherRank = other.rankOf(feature);
            if (otherRank > 0) {
                final int myRank = e.getValue();
                score += BASE_RANK_SCORE - 2 * myRank - Math.abs(myRank - otherRank);
            }
        }
        return new MovieScore(other, score);
    }

    @Override
    public String toString()
    {
        return String.format("%08x", id);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Movie movie = (Movie)o;
        return id == movie.id;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id);
    }
}
