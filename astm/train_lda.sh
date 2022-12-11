cd mallet-2.0.8

./bin/mallet import-file --input ../data/so_java.txt --output ./output/topic-modeling-input-java.mallet --keep-sequence --remove-stopwords

./bin/mallet train-topics --input ./output/topic-modeling-input-java.mallet --num-topics 50 --random-seed 7 \
--output-topic-keys ./output/topic-keys-java-50.txt --output-doc-topics ./output/doc-topics-java-50.txt \
--word-topic-counts-file ./output/word-topic-counts-file-java-50.txt --output-model ./output/output-model-java-50 \
--output-state ./output/topic-state-java-50.gz --output-topic-docs ./output/output-topic-docs-java-50.txt \
--diagnostics-file ./output/diagnostics-file-java-50.xml --optimize-interval 20

./bin/mallet train-topics --input ./output/topic-modeling-input-java.mallet --num-topics 100 --random-seed 7 \
--output-topic-keys ./output/topic-keys-java-100.txt --output-doc-topics ./output/doc-topics-java-100.txt \
--word-topic-counts-file ./output/word-topic-counts-file-java-100.txt --output-model ./output/output-model-java-100 \
--output-state ./output/topic-state-java-100.gz --output-topic-docs ./output/output-topic-docs-java-100.txt \
--diagnostics-file ./output/diagnostics-file-java-100.xml --optimize-interval 20

./bin/mallet train-topics --input ./output/topic-modeling-input-java.mallet --num-topics 150 --random-seed 7 \
--output-topic-keys ./output/topic-keys-java-150.txt \
--word-topic-counts-file ./output/word-topic-counts-file-java-150.txt --optimize-interval 20

./bin/mallet train-topics --input ./output/topic-modeling-input-java.mallet --num-topics 200 --random-seed 7 \
--output-topic-keys ./output/topic-keys-java-200.txt \
--word-topic-counts-file ./output/word-topic-counts-file-java-200.txt --optimize-interval 20

######################################################################################################################

./bin/mallet import-file --input ../data/so_android.txt --output ./output/topic-modeling-input-android.mallet --keep-sequence --remove-stopwords

./bin/mallet train-topics --input ./output/topic-modeling-input-android.mallet --num-topics 50 --random-seed 7 \
--output-topic-keys ./output/topic-keys-android-50.txt \
--word-topic-counts-file ./output/word-topic-counts-file-android-50.txt --optimize-interval 20

./bin/mallet train-topics --input ./output/topic-modeling-input-android.mallet --num-topics 100 --random-seed 7 \
--output-topic-keys ./output/topic-keys-android-100.txt \
--word-topic-counts-file ./output/word-topic-counts-file-android-100.txt --optimize-interval 20

./bin/mallet train-topics --input ./output/topic-modeling-input-android.mallet --num-topics 150 --random-seed 7 \
--output-topic-keys ./output/topic-keys-android-150.txt \
--word-topic-counts-file ./output/word-topic-counts-file-android-150.txt --optimize-interval 20

./bin/mallet train-topics --input ./output/topic-modeling-input-android.mallet --num-topics 200 --random-seed 7 \
--output-topic-keys ./output/topic-keys-android-200.txt \
--word-topic-counts-file ./output/word-topic-counts-file-android-200.txt --optimize-interval 20

#######################################################################################################################

./bin/mallet import-file --input ../data/so_c#.txt --output ./output/topic-modeling-input-c#.mallet --keep-sequence --remove-stopwords

./bin/mallet train-topics --input ./output/topic-modeling-input-c#.mallet --num-topics 50 --random-seed 7 \
--output-topic-keys ./output/topic-keys-c#-50.txt \
--word-topic-counts-file ./output/word-topic-counts-file-c#-50.txt --optimize-interval 20

./bin/mallet train-topics --input ./output/topic-modeling-input-c#.mallet --num-topics 100 --random-seed 7 \
--output-topic-keys ./output/topic-keys-c#-100.txt \
--word-topic-counts-file ./output/word-topic-counts-file-c#-100.txt --optimize-interval 20

./bin/mallet train-topics --input ./output/topic-modeling-input-c#.mallet --num-topics 150 --random-seed 7 \
--output-topic-keys ./output/topic-keys-c#-150.txt \
--word-topic-counts-file ./output/word-topic-counts-file-c#-150.txt --optimize-interval 20

./bin/mallet train-topics --input ./output/topic-modeling-input-c#.mallet --num-topics 200 --random-seed 7 \
--output-topic-keys ./output/topic-keys-c#-200.txt \
--word-topic-counts-file ./output/word-topic-counts-file-c#-200.txt --optimize-interval 20
