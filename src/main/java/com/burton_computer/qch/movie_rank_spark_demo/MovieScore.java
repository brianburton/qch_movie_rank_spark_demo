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

import javax.annotation.concurrent.Immutable;
import java.io.Serializable;
import java.util.Comparator;

@Immutable
public class MovieScore
    implements Serializable
{
    private final Movie movie;
    private final int score;

    public MovieScore(Movie movie,
                      int score)
    {
        this.movie = movie;
        this.score = score;
    }

    public Movie getMovie()
    {
        return movie;
    }

    public int getScore()
    {
        return score;
    }

    public int getMovieId()
    {
        return movie.getId();
    }

    @Override
    public String toString()
    {
        return movie.toString() + "=" + score;
    }

    public static class SortByScore
        implements Comparator<MovieScore>,
                   Serializable
    {
        @Override
        public int compare(MovieScore o1,
                           MovieScore o2)
        {
            int scoreDiff = o1.getScore() - o2.getScore();
            if (scoreDiff != 0) {
                return -scoreDiff;
            } else {
                return o1.getMovieId() - o2.getMovieId();
            }
        }
    }
}
