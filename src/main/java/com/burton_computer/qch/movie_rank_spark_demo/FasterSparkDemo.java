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

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableSet;
import org.javimmutable.collections.util.JImmutables;
import scala.Tuple2;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

public class FasterSparkDemo
{
    public static void main(String[] argv)
        throws Exception
    {
        final int numFeatures, numMovies;
        if (argv.length == 0) {
            numFeatures = 500;
            numMovies = 10000;
        } else if (argv.length == 2) {
            numFeatures = Integer.parseInt(argv[0]);
            numMovies = Integer.parseInt(argv[1]);
        } else {
            System.err.println("usage: SparkDemo [numFeatures numMovies]");
            System.exit(1);
            return;
        }

        final Random r = new Random(42);
        final JImmutableList<Feature> allFeatures = createFeatures(numFeatures);
        final JImmutableList<Movie> allMovies = createMovies(allFeatures, r, numMovies);
        final String filename = "_fast";
        final int numPartitions = 500;

        final SparkConf conf = new SparkConf(true)
            .setAppName("Queen City Hacks Demo - Fast Version")
            .setMaster("spark://192.168.84.15:7077")
            .setJars(new String[]{"target/qch_movie_rank_spark_demo-1.0-SNAPSHOT.jar"});

        // stage 0
        final JavaRDD<Movie> movies = new JavaSparkContext(conf).parallelize(allMovies.getList());
        final JavaPairRDD<Feature, Movie> moviesByFeature1 = movies.flatMapToPair(movie -> convertToFeaturePairs(movie).iterator());

        // stage 1
        final JavaPairRDD<Feature, JImmutableSet<Movie>> moviesByFeature2 = moviesByFeature1.aggregateByKey(JImmutables.set(),
                                                                                                            numPartitions,
                                                                                                            (a, b) -> a.insert(b),
                                                                                                            (a, b) -> a.union(b));
        final JavaPairRDD<Movie, JImmutableSet<Movie>> relatedMoviesByMovie1 = moviesByFeature2.flatMapToPair(t -> flipFeatureSet(t._2()).iterator());
        final JavaPairRDD<Movie, JImmutableSet<MovieScore>> rawScores = relatedMoviesByMovie1.mapToPair(tuple -> scoreMovies(tuple._1(), tuple._2()));

        // stage 2
        final JavaPairRDD<Movie, JImmutableSet<MovieScore>> combinedScores = rawScores.reduceByKey((a, b) -> a.insertAll(b));

        writeResultsToFile(filename, combinedScores);
        System.out.println("press enter to exit");
        System.in.read();
    }

    private static Tuple2<Movie, JImmutableSet<MovieScore>> scoreMovies(Movie movie,
                                                                        JImmutableSet<Movie> movies)
    {
        JImmutableSet<MovieScore> scored = JImmutables.sortedSet(new MovieScore.SortByScore());
        return Tuple2.apply(movie, movies.transform(scored, other -> movie.computeScoreFor(other)));
    }

    private static JImmutableList<Tuple2<Feature, Movie>> convertToFeaturePairs(Movie movie)
    {
        return movie.getFeatures().stream()
            .map(feature -> Tuple2.apply(feature, movie))
            .collect(JImmutables.<Tuple2<Feature, Movie>>list().listCollector());
    }

    private static void writeResultsToFile(String filename,
                                           JavaPairRDD<Movie, JImmutableSet<MovieScore>> combinedScores)
        throws IOException
    {
        try (BufferedWriter out = new BufferedWriter(new FileWriter(filename))) {
            for (Tuple2<Movie, JImmutableSet<MovieScore>> tuple : combinedScores.collect()) {
                out.write(String.format("%s => %s%n", tuple._1(), tuple._2()));
            }
        }
    }

    private static Tuple2<Movie, MovieScore> computeScoreForPair(Tuple2<Movie, Movie> tuple)
    {
        Movie first = tuple._1();
        Movie related = tuple._2();
        return Tuple2.apply(first, first.computeScoreFor(related));
    }

    private static JImmutableList<Tuple2<Movie, Movie>> explodeMovieSets(Tuple2<Movie, JImmutableSet<Movie>> tuple)
    {
        Movie first = tuple._1();
        JImmutableSet<Movie> relatedMovies = tuple._2();
        return relatedMovies.stream()
            .map(related -> Tuple2.apply(first, related))
            .collect(JImmutables.<Tuple2<Movie, Movie>>list().listCollector());
    }

    private static JImmutableList<Tuple2<Movie, JImmutableSet<Movie>>> flipFeatureSet(JImmutableSet<Movie> movies)
    {
        return movies.stream()
            .map(movie -> Tuple2.apply(movie, movies.delete(movie)))
            .collect(JImmutables.<Tuple2<Movie, JImmutableSet<Movie>>>list().listCollector());
    }

    private static JImmutableList<Movie> createMovies(@Nonnull JImmutableList<Feature> allFeatures,
                                                      @Nonnull Random r,
                                                      int numberToCreate)
    {
        return IntStream.range(1, 1 + numberToCreate)
            .boxed()
            .map(i -> createMovie(allFeatures, r, i))
            .collect(JImmutables.<Movie>list().listCollector());
    }

    private static Movie createMovie(@Nonnull JImmutableList<Feature> allFeatures,
                                     @Nonnull Random r,
                                     int movieId)
    {
        return new Movie(movieId, selectFeatures(allFeatures, r, Movie.NUM_FEATURES));
    }

    private static JImmutableList<Feature> selectFeatures(@Nonnull JImmutableList<Feature> allFeatures,
                                                          @Nonnull Random r,
                                                          int numberToSelect)
    {
        Set<Feature> selected = new LinkedHashSet<>();
        while (selected.size() < numberToSelect) {
            selected.add(allFeatures.get(r.nextInt(allFeatures.size())));
        }
        return JImmutables.list(selected);
    }

    private static JImmutableList<Feature> createFeatures(int numberToCreate)
    {
        return IntStream.range(1, 1 + numberToCreate)
            .boxed()
            .map(i -> new Feature(i))
            .collect(JImmutables.<Feature>list().listCollector());
    }
}
