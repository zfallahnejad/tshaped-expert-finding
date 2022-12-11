# Attention-based Skill Translation

## Download Posts.xml
Here is the [link](https://drive.google.com/file/d/1OPBCLwgTCkzCzdpN_F4O5TuSCPtA3IdP/view?usp=sharing) to our dataset.
Download this 7-Gigabyte 7z file which contains StackOverflow `Posts.xml` file and extract it inside `data` directory.
 
## Clusters
We used dataset shared by [Rostami et al.](https://github.com/prostami/tshaped-Mining)
Download `JavaCluster.csv`, `C#Cluster.csv`, and `AndroidCluster.csv` and put them inside `data` folder.
 
## Stackoverflow collections
For a single tag like `java`, we extract questions of stackoverflow which tagged by `java` and their answers.
For each answer, we take the tags of its parent and append it to the data of that answer, So that the new xml file
contains tags for questions and answers.
Finally, this script load each stack overflow collection, normalize its text and prepare 
another file (for example `so_java.txt` for java collection) to be used as the input of other scripts.
```
python3 prepare_so_collection.py
```

## Train LDA
```
tar -zxvf mallet-2.0.8.tar.gz
mkdir ./mallet-2.0.8/output
```
Before start training, you should change value of `MEMORY=1g` inside `./mallet-2.0.8/bin/mallet` file to `MEMORY=32g`.
Then, run the following script
```
sh train_lda.sh
```

## Build word topic vectors
We will use output of previously trained lda to build vectors for each words.
```
python3 build_word_topic_vectors.py --tag java --embed_size 50
python3 build_word_topic_vectors.py --tag java --embed_size 100
python3 build_word_topic_vectors.py --tag java --embed_size 150
python3 build_word_topic_vectors.py --tag java --embed_size 200
python3 build_word_topic_vectors.py --tag android --embed_size 50
python3 build_word_topic_vectors.py --tag android --embed_size 100
python3 build_word_topic_vectors.py --tag android --embed_size 150
python3 build_word_topic_vectors.py --tag android --embed_size 200
python3 build_word_topic_vectors.py --tag c# --embed_size 50
python3 build_word_topic_vectors.py --tag c# --embed_size 100
python3 build_word_topic_vectors.py --tag c# --embed_size 150
python3 build_word_topic_vectors.py --tag c# --embed_size 200
```

## Prepare astm inputs 
Run the following scripts to generate input files which will be used for training astm networks:
```python3
python3 prepare_astm_inputs.py --dataset java --data_path ../data/so_java.txt --skill_cluster_path ./data/JavaCluster.csv --so_input_path ../data/JavaPosts.xml --result_path ./data/ --train_part 1.0 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 50
python3 prepare_astm_inputs.py --dataset java --data_path ../data/so_java.txt --skill_cluster_path ./data/JavaCluster.csv --so_input_path ../data/JavaPosts.xml --result_path ./data/ --train_part 1.0 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 100
python3 prepare_astm_inputs.py --dataset java --data_path ../data/so_java.txt --skill_cluster_path ./data/JavaCluster.csv --so_input_path ../data/JavaPosts.xml --result_path ./data/ --train_part 1.0 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 150
python3 prepare_astm_inputs.py --dataset java --data_path ../data/so_java.txt --skill_cluster_path ./data/JavaCluster.csv --so_input_path ../data/JavaPosts.xml --result_path ./data/ --train_part 1.0 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 200
python3 prepare_astm_inputs.py --dataset java --data_path ../data/so_java.txt --skill_cluster_path ./data/JavaCluster.csv --so_input_path ../data/JavaPosts.xml --result_path ./data/ --train_part 0.75 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 50
python3 prepare_astm_inputs.py --dataset java --data_path ../data/so_java.txt --skill_cluster_path ./data/JavaCluster.csv --so_input_path ../data/JavaPosts.xml --result_path ./data/ --train_part 0.75 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 100
python3 prepare_astm_inputs.py --dataset java --data_path ../data/so_java.txt --skill_cluster_path ./data/JavaCluster.csv --so_input_path ../data/JavaPosts.xml --result_path ./data/ --train_part 0.75 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 150
python3 prepare_astm_inputs.py --dataset java --data_path ../data/so_java.txt --skill_cluster_path ./data/JavaCluster.csv --so_input_path ../data/JavaPosts.xml --result_path ./data/ --train_part 0.75 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 200

python3 prepare_astm_inputs.py --dataset android --data_path ../data/so_android.txt --skill_cluster_path ./data/AndroidCluster.csv --so_input_path ../data/AndroidPosts.xml --result_path ./data/ --train_part 1.0 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 50
python3 prepare_astm_inputs.py --dataset android --data_path ../data/so_android.txt --skill_cluster_path ./data/AndroidCluster.csv --so_input_path ../data/AndroidPosts.xml --result_path ./data/ --train_part 1.0 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 100
python3 prepare_astm_inputs.py --dataset android --data_path ../data/so_android.txt --skill_cluster_path ./data/AndroidCluster.csv --so_input_path ../data/AndroidPosts.xml --result_path ./data/ --train_part 1.0 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 150
python3 prepare_astm_inputs.py --dataset android --data_path ../data/so_android.txt --skill_cluster_path ./data/AndroidCluster.csv --so_input_path ../data/AndroidPosts.xml --result_path ./data/ --train_part 1.0 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 200
python3 prepare_astm_inputs.py --dataset android --data_path ../data/so_android.txt --skill_cluster_path ./data/AndroidCluster.csv --so_input_path ../data/AndroidPosts.xml --result_path ./data/ --train_part 0.75 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 50
python3 prepare_astm_inputs.py --dataset android --data_path ../data/so_android.txt --skill_cluster_path ./data/AndroidCluster.csv --so_input_path ../data/AndroidPosts.xml --result_path ./data/ --train_part 0.75 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 100
python3 prepare_astm_inputs.py --dataset android --data_path ../data/so_android.txt --skill_cluster_path ./data/AndroidCluster.csv --so_input_path ../data/AndroidPosts.xml --result_path ./data/ --train_part 0.75 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 150
python3 prepare_astm_inputs.py --dataset android --data_path ../data/so_android.txt --skill_cluster_path ./data/AndroidCluster.csv --so_input_path ../data/AndroidPosts.xml --result_path ./data/ --train_part 0.75 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 200

python3 prepare_astm_inputs.py --dataset c# --data_path ../data/so_c#.txt --skill_cluster_path ./data/C#Cluster.csv --so_input_path ../data/C#Posts.xml --result_path ./data/ --train_part 1.0 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 50
python3 prepare_astm_inputs.py --dataset c# --data_path ../data/so_c#.txt --skill_cluster_path ./data/C#Cluster.csv --so_input_path ../data/C#Posts.xml --result_path ./data/ --train_part 1.0 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 100
python3 prepare_astm_inputs.py --dataset c# --data_path ../data/so_c#.txt --skill_cluster_path ./data/C#Cluster.csv --so_input_path ../data/C#Posts.xml --result_path ./data/ --train_part 1.0 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 150
python3 prepare_astm_inputs.py --dataset c# --data_path ../data/so_c#.txt --skill_cluster_path ./data/C#Cluster.csv --so_input_path ../data/C#Posts.xml --result_path ./data/ --train_part 1.0 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 200
python3 prepare_astm_inputs.py --dataset c# --data_path ../data/so_c#.txt --skill_cluster_path ./data/C#Cluster.csv --so_input_path ../data/C#Posts.xml --result_path ./data/ --train_part 0.75 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 50
python3 prepare_astm_inputs.py --dataset c# --data_path ../data/so_c#.txt --skill_cluster_path ./data/C#Cluster.csv --so_input_path ../data/C#Posts.xml --result_path ./data/ --train_part 0.75 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 100
python3 prepare_astm_inputs.py --dataset c# --data_path ../data/so_c#.txt --skill_cluster_path ./data/C#Cluster.csv --so_input_path ../data/C#Posts.xml --result_path ./data/ --train_part 0.75 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 150
python3 prepare_astm_inputs.py --dataset c# --data_path ../data/so_c#.txt --skill_cluster_path ./data/C#Cluster.csv --so_input_path ../data/C#Posts.xml --result_path ./data/ --train_part 0.75 --post_type question --word_topic_vectors_path ../word_vectors/word_topic_vectors/ --num_words 5000 --word_vector_dim 200
```

## train astm networks
```
sh run.sh
```
