import os
import time
import random
import pickle
import argparse
import statistics
import tensorflow
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.style as style
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import MultiLabelBinarizer
from tensorflow.keras.models import Model
from tensorflow.keras.initializers import glorot_uniform
from tensorflow.keras.preprocessing.sequence import pad_sequences
from tensorflow.keras.callbacks import ModelCheckpoint, ReduceLROnPlateau, EarlyStopping, TensorBoard
from model_tf2 import SentenceClassifier, fbeta, macro_f1, evaluation, evaluation_top_label, perf_grid, \
    performance_table, performance_curves, top_words, top_words_2, integrate_scores, integrate_scores_2, \
    AttentionWithContext, AttentionWithContextV2

random.seed(7)
CWD = os.path.dirname(__file__)


def print_time(t):
    """Function that converts time period in seconds into %h:%m:%s expression.
    Args:
        t (int): time period in seconds
    Returns:
        s (string): time period formatted
    """
    h = t // 3600
    m = (t % 3600) // 60
    s = (t % 3600) % 60
    return '%dh:%dm:%ds' % (h, m, s)


def load_skill_area_tag(cluster_path):
    if not os.path.exists(cluster_path):
        print("{} not exists!".format(cluster_path))
        exit(1)
    skill_tags, tag_skill = {}, {}
    with open(cluster_path) as infile:
        next(infile)
        for line in infile:
            skill_area, tag = line.strip().split(',')
            if skill_area in skill_tags:
                skill_tags[skill_area].append(tag)
            else:
                skill_tags[skill_area] = [tag]
            tag_skill[tag] = skill_area
    return skill_tags, tag_skill


def so_post_sampling(tag_skill, samples_path, post_type, log_file):
    print("tags:", set(tag_skill.keys()))
    with open(samples_path, "rb") as input_file:
        sample_post_tags = pickle.load(input_file)

    print("#total sample {}s: {}".format(post_type, len(sample_post_tags)))
    log_file.write("#total sample {}s: {}\n".format(post_type, len(sample_post_tags)))

    all_tags = [tag for pid, tags in sample_post_tags.items() for tag in tags]
    for t in tag_skill:
        print(t, all_tags.count(t), len(all_tags))
    return sample_post_tags


def prepare_inputs(dataset, data_path, sample_post_tags, word_index_dir, train_part, post_type, word_vector_dim,
                   num_words, max_sentence_len, word_topic_vectors_path, log_file):
    word_index_path = os.path.join(word_index_dir, "sa_word_index_{}_{}s_tr{}_{}d_{}wr.pkl".format(
        dataset, post_type, train_part, word_vector_dim, num_words))
    if not os.path.exists(word_index_path):
        print("{} not exists!".format(word_index_path))
        exit(1)

    with open(word_index_path, "rb") as input_file:
        word_index = pickle.load(input_file)
    print("Unique Tokens in Training Data: ", len(word_index))

    sequences, labels = [], []
    post_index_id = {}
    with open(data_path, encoding="utf8") as infile:
        for line in infile:
            line = line.strip().split()
            post_id = line[0]
            if post_id not in sample_post_tags:
                continue

            post_text = line[1:]
            post_word_indices = [word_index.get(i) for i in post_text]
            post_word_indices = [_ for _ in post_word_indices if _]
            if len(post_word_indices) == 0:
                continue

            post_index_id[len(sequences)] = post_id
            sequences.append(post_word_indices)
            labels.append(sample_post_tags[post_id])
    print("len(sequences)={}, len(labels)={}".format(len(sequences), len(labels)))
    log_file.write("len(sequences)={}, len(labels)={}\n".format(len(sequences), len(labels)))

    multilabel_binarizer = MultiLabelBinarizer()
    multilabel_binarizer.fit(labels)
    print("{} classes: {}".format(len(multilabel_binarizer.classes_), multilabel_binarizer.classes_))
    log_file.write("{} classes: {}\n".format(len(multilabel_binarizer.classes_), multilabel_binarizer.classes_))
    labels = multilabel_binarizer.transform(labels)

    sequences_length = [len(s) for s in sequences]
    length_count = {l: sequences_length.count(l) for l in set(sequences_length)}
    print("Average length=", statistics.mean(sequences_length))
    print("Median length=", statistics.median(sequences_length))
    print("Minimum length={}, sequence count={}".format(min(sequences_length), length_count[min(sequences_length)]))
    print("Maximum length={}, sequence count={}".format(max(sequences_length), length_count[max(sequences_length)]))
    max_count = max(length_count.values())
    length_max_count = list(length_count.keys())[list(length_count.values()).index(max_count)]
    print("Maximum count={}, on which length={}".format(max_count, length_max_count))
    data = pad_sequences(sequences, maxlen=max_sentence_len, padding='post')

    vocab_size = len(word_index) + 1  # Adding again 1 because of reserved 0 index
    embedding_matrix = np.zeros((vocab_size, word_vector_dim))
    with open(os.path.join(word_topic_vectors_path, "word_topic_vectors_{}_{}d.txt".format(dataset, word_vector_dim)),
              encoding="utf8") as infile:
        for line in infile:
            line = line.strip().split('\t')
            word = line[0]
            if word not in word_index:
                continue
            vector = [float(i) for i in line[1].split(' ')]
            embedding_matrix[word_index[word]] = np.array(vector)
    nonzero_elements = np.count_nonzero(np.count_nonzero(embedding_matrix, axis=1))
    print("number of non zero row of embedding matrix: {}".format(nonzero_elements / vocab_size))

    return data, labels, word_index, multilabel_binarizer, post_index_id, embedding_matrix


def plot_1(history, result_path):
    style.use("bmh")
    plt.figure(figsize=(15, 15))

    # summarize history for macro f1
    # plt.subplot(411)
    plt.subplot(3, 2, 1)
    plt.plot(history.history['macro_f1'])
    plt.plot(history.history['val_macro_f1'])
    plt.title('model macro f1')
    plt.ylabel('macro f1')
    plt.xlabel('epoch')
    plt.legend(['train', 'test'], loc='upper left')
    # summarize history for categorical accuracy
    # plt.subplot(412)
    plt.subplot(3, 2, 2)
    plt.plot(history.history['categorical_accuracy'])
    plt.plot(history.history['val_categorical_accuracy'])
    plt.title('model categorical accuracy')
    plt.ylabel('categorical accuracy')
    plt.xlabel('epoch')
    plt.legend(['train', 'test'], loc='upper left')
    # summarize history for loss
    # plt.subplot(413)
    plt.subplot(3, 2, 3)
    plt.plot(history.history['acc'])
    plt.plot(history.history['val_acc'])
    plt.title('model accuracy')
    plt.ylabel('accuracy')
    plt.xlabel('epoch')
    plt.legend(['train', 'test'], loc='upper left')
    # summarize history for loss
    # plt.subplot(414)
    plt.subplot(3, 2, 4)
    plt.plot(history.history['loss'])
    plt.plot(history.history['val_loss'])
    plt.title('model loss')
    plt.ylabel('loss')
    plt.xlabel('epoch')
    plt.legend(['train', 'test'], loc='upper left')
    # summarize history for top_k_categorical_accuracy
    plt.subplot(3, 2, 5)
    plt.plot(history.history['top_k_categorical_accuracy'])
    plt.plot(history.history['val_top_k_categorical_accuracy'])
    plt.title('model top k categorical accuracy')
    plt.ylabel('top_k_categorical_accuracy')
    plt.xlabel('epoch')
    plt.legend(['train', 'test'], loc='upper left')
    # summarize history for fbeta
    plt.subplot(3, 2, 6)
    plt.plot(history.history['fbeta'])
    plt.plot(history.history['val_fbeta'])
    plt.title('model fbeta')
    plt.ylabel('fbeta')
    plt.xlabel('epoch')
    plt.legend(['train', 'test'], loc='upper left')
    plt.savefig(os.path.join(result_path, "plot_1.png"))
    plt.close()


def learning_curves(history, result_path):
    """Plot the learning curves of loss and macro f1 score
    for the training and validation datasets.

    Args:
        history: history callback of fitting a model
    """
    loss = history.history['loss']
    val_loss = history.history['val_loss']
    macro_f1 = history.history['macro_f1']
    val_macro_f1 = history.history['val_macro_f1']
    epochs = len(loss)

    style.use("bmh")
    plt.figure(figsize=(8, 8))
    plt.subplot(2, 1, 1)
    plt.plot(range(1, epochs + 1), loss, label='Training Loss')
    plt.plot(range(1, epochs + 1), val_loss, label='Validation Loss')
    plt.legend(loc='upper right')
    plt.ylabel('Loss')
    plt.title('Training and Validation Loss')

    plt.subplot(2, 1, 2)
    plt.plot(range(1, epochs + 1), macro_f1, label='Training Macro F1-score')
    plt.plot(range(1, epochs + 1), val_macro_f1, label='Validation Macro F1-score')
    plt.legend(loc='lower right')
    plt.ylabel('Macro F1-score')
    plt.title('Training and Validation Macro F1-score')
    plt.xlabel('epoch')

    plt.savefig(os.path.join(result_path, "plot_2.png"))

    return loss, val_loss, macro_f1, val_macro_f1


def train(dataset, data_path, skill_cluster_path, samples_path, word_topic_vectors_path, result_path, train_part,
          post_type, num_words, word_vector_dim, train_embedding, max_sentence_len, epochs, batch_size, model_name,
          continue_training, initial_epoch, num_top_trans, lstm_dim, dropout_value, word_index_dir):
    if not os.path.exists(result_path):
        os.mkdir(result_path)
    log_file = open(os.path.join(result_path, "log.txt"), "w")

    skill_tags, tag_skill = load_skill_area_tag(skill_cluster_path)
    # skill_areas = sorted(list(skill_tags.keys()))
    sample_post_tags = so_post_sampling(tag_skill, samples_path, post_type, log_file)
    data, labels, word_index, multilabel_binarizer, post_index_id, embedding_matrix = prepare_inputs(
        dataset, data_path, sample_post_tags, word_index_dir, train_part, post_type, word_vector_dim, num_words,
        max_sentence_len, word_topic_vectors_path, log_file
    )
    print("Using trained word embedding with shape: {}".format(embedding_matrix.shape))
    log_file.write("Using trained word embedding with shape: {}\n".format(embedding_matrix.shape))

    index_word = {index: word for word, index in word_index.items()}

    print('Shape of data tensor: {}\nShape of labels tensor: {}\n'.format(data.shape, labels.shape))
    log_file.write('Shape of data tensor: {}\nShape of labels tensor: {}\n'.format(data.shape, labels.shape))

    x_train, x_test, y_train, y_test = train_test_split(data, labels, test_size=0.2, random_state=9000)
    print("x_train shape: {}, y_train shape: {}".format(x_train.shape, y_train.shape))
    print("x_test shape: {}, y_test shape: {}".format(x_test.shape, y_test.shape))
    log_file.write("x_train shape: {}, y_train shape: {}\n".format(x_train.shape, y_train.shape))
    log_file.write("x_test shape: {}, y_test shape: {}\n".format(x_test.shape, y_test.shape))

    np.random.seed(7)
    tensorflow.compat.v1.set_random_seed(20)
    num_classes = labels.shape[1]
    classifier = SentenceClassifier(max_sentence_len=max_sentence_len, num_words=num_words,
                                    word_vector_dim=word_vector_dim, label_count=num_classes,
                                    word_index=word_index, label_encoder=multilabel_binarizer)
    print("Train word embedding: {}".format(train_embedding))
    log_file.write("Train word embedding: {}\n".format(train_embedding))

    loss_function = 'binary_crossentropy'
    print("loss function: {}".format(loss_function))
    log_file.write("loss function: {}\n".format(loss_function))

    if continue_training:
        model = tensorflow.keras.models.load_model(
            os.path.join(result_path, 'model.h5'),
            custom_objects={
                "macro_f1": macro_f1, "fbeta": fbeta, "AttentionWithContext": AttentionWithContext,
                "AttentionWithContextV2": AttentionWithContextV2, 'GlorotUniform': glorot_uniform()
            })
    else:
        if model_name == "astm1":
            model = classifier.astm_1(train_embedding, embedding_matrix, lstm_dim, dropout_value)
        elif model_name == "astm2":
            model = classifier.astm_2(train_embedding, embedding_matrix, lstm_dim, dropout_value)
        else:
            exit(1)

        model.compile(loss=loss_function, optimizer='adam',
                      metrics=[macro_f1, 'acc', 'categorical_accuracy', 'top_k_categorical_accuracy', fbeta])

    print("Model Summary:\n{}".format(model.summary()))
    model.summary(print_fn=lambda x: log_file.write(x + '\n'))
    # log_file.write("Model Summary:\n{}".format(model.summary()))
    # plot_model(model, to_file='model_plot.png', show_shapes=True, show_layer_names=True)

    print("\nTraining model...")
    log_file.write("\nTraining model...\n")

    callbacks = [
        ReduceLROnPlateau(verbose=1),
        EarlyStopping(patience=3, verbose=1),
        ModelCheckpoint(filepath=os.path.join(result_path, 'model.h5'), save_best_only=True, verbose=1),
        TensorBoard(log_dir=os.path.join(result_path, "logs"))
    ]
    start = time.time()
    print("Without class_weight!\n")
    log_file.write("Without class_weight!\n")
    history = model.fit(x_train, y_train, batch_size=batch_size, epochs=epochs, validation_split=0.1,
                        callbacks=callbacks, initial_epoch=initial_epoch)
    print('\nTraining took {}'.format(print_time(int(time.time() - start))))
    log_file.write('\nTraining took {}\n'.format(print_time(int(time.time() - start))))

    if model_name == "astm1":
        print("Extract top words of test data:")
        log_file.write("Extract top words of test data:\n")
        test_word_scores = top_words(model, x_test, y_test, "Test", multilabel_binarizer.classes_, index_word, 64,
                                     log_file, num_top_trans)

        print("Extract top words of train data:")
        log_file.write("Extract top words of train data:\n")
        train_word_scores = top_words(model, x_train, y_train, "Train", multilabel_binarizer.classes_, index_word, 64,
                                      log_file, num_top_trans)
        integrate_scores(train_word_scores, test_word_scores, multilabel_binarizer.classes_, log_file)
    elif model_name == "astm2":
        print("Extract top words of test data:")
        log_file.write("Extract top words of test data:\n")
        top_words_2(model, x_test, y_test, "Test", multilabel_binarizer.classes_, index_word, 64, log_file, result_path,
                    num_top_trans)

        print("Extract top words of train data:")
        log_file.write("Extract top words of train data:\n")
        top_words_2(model, x_train, y_train, "Train", multilabel_binarizer.classes_, index_word, 64, log_file,
                    result_path, num_top_trans)
        integrate_scores_2(multilabel_binarizer.classes_, log_file, result_path)

    if history.history:
        plot_1(history, result_path)
        losses, val_losses, macro_f1s, val_macro_f1s = learning_curves(history, result_path)
        print("Validation Macro Loss: %.2f" % val_losses[-1])
        print("Validation Macro F1-score: %.2f" % val_macro_f1s[-1])
        log_file.write("Validation Macro Loss: %.2f\n" % val_losses[-1])
        log_file.write("Validation Macro F1-score: %.2f\n" % val_macro_f1s[-1])

    evaluation(model, x_test, y_test, log_file)
    # evaluation_top_label(model, x_test, y_test, log_file)

    # if model_name == "astm1":
    #     intermediate_layer_model = Model(inputs=model.input, outputs=model.get_layer('attention_with_context').output)
    #
    #     print("Extract words scores of data:")
    #     weighted_seq, attention = intermediate_layer_model.predict(data, batch_size=64)
    #     print("attention shape: {}".format(attention.shape))
    #     score_file = os.path.join(result_path, "data_scores_{}_tr{}.txt".format(post_type, train_part))
    #     with open(score_file, "w", encoding='utf8') as out_file:
    #         for i, sample in enumerate(data):
    #             sample_scores = []
    #             sample_labels = [multilabel_binarizer.classes_[j] for j, l in enumerate(labels[i]) if l == 1]
    #             for k, word_index in enumerate(sample):
    #                 if word_index == 0:
    #                     continue
    #                 word = index_word[word_index]
    #                 score = attention[i][k]
    #                 sample_scores.append((word, score))
    #             out_file.write("{}\t{}\t{}\n".format(post_index_id[i], sample_labels, sample_scores))
    # elif model_name == "astm2":
    #     intermediate_layer_model = Model(inputs=model.input,
    #                                      outputs=model.get_layer('attention_with_context_v2').output)
    #
    #     last_index = 0
    #     num_parts = 100
    #     print("Extract words scores of data:")
    #     score_file = os.path.join(result_path, "data_scores_{}_tr{}.txt".format(post_type, train_part))
    #     with open(score_file, "w", encoding='utf8') as out_file:
    #         for p, xp in enumerate(np.array_split(data, num_parts)):
    #             print("x part {} shape: {}".format(p, xp.shape))
    #             weighted_seq, attention = intermediate_layer_model.predict(xp, batch_size=64)
    #             del weighted_seq
    #             print("attention shape: {}".format(attention.shape))
    #             log_file.write("attention shape: {}\n".format(attention.shape))
    #             for i, sample in enumerate(xp):
    #                 sample_labels = [j for j, l in enumerate(labels[last_index]) if l == 1]
    #                 sample_label_scores = {l: [] for l in sample_labels}
    #                 for k, word_index in enumerate(sample):
    #                     if word_index == 0:
    #                         continue
    #                     word = index_word[word_index]
    #                     for label in sample_labels:
    #                         score = attention[i][label][k]
    #                         sample_label_scores[label].append((word, score))
    #                 for l in sample_labels:
    #                     out_file.write("{}\t{}\t{}\t{}\n".format(
    #                         post_index_id[last_index], [multilabel_binarizer.classes_[l] for l in sample_labels],
    #                         multilabel_binarizer.classes_[l], sample_label_scores[l]))
    #                 last_index += 1

    max_perf, grid = performance_table(x_test, y_test, multilabel_binarizer.classes_, model, result_path, log_file)
    performance_curves(max_perf, grid, model, multilabel_binarizer.classes_, x_test, result_path)

    log_file.close()


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--dataset', help="", default='java')
    parser.add_argument('--data_path', help='', required=True)
    parser.add_argument('--skill_cluster_path', help='', required=True)
    parser.add_argument('--samples_path', help='', required=True)
    parser.add_argument('--word_topic_vectors_path', required=True)
    parser.add_argument('--word_index_dir', help='', required=True)
    parser.add_argument('--result_path', help='', required=True)
    parser.add_argument('--train_part', type=float, help="0.5, 0.75 or 1.0", required=True)
    parser.add_argument('--post_type', help="post, question or answer", required=True)
    parser.add_argument('--num_words', type=int, help="", required=True)
    parser.add_argument('--word_vector_dim', type=int, help="", required=True)
    parser.add_argument('--train_embedding', type=bool, help="", default="True")
    parser.add_argument('--max_sentence_len', type=int, help="", required=True)
    parser.add_argument('--epochs', type=int, help="", required=True)
    parser.add_argument('--batch_size', type=int, help="", required=True)
    parser.add_argument('--model_name', help="", required=True)
    parser.add_argument('--continue_training', help="", action='store_true')
    parser.add_argument('--initial_epoch', type=int, help='', default=0)
    parser.add_argument('--num_top_trans', type=int, help='', default=200)
    parser.add_argument('--lstm_dim', type=int, help='', default=128)
    parser.add_argument('--dropout_value', type=float, help='', default=0.15)
    args = parser.parse_args()

    train(
        args.dataset, args.data_path, args.skill_cluster_path, args.samples_path, args.word_topic_vectors_path,
        args.result_path, args.train_part, args.post_type, args.num_words, args.word_vector_dim, args.train_embedding,
        args.max_sentence_len, args.epochs, args.batch_size, args.model_name, args.continue_training,
        args.initial_epoch, args.num_top_trans, args.lstm_dim, args.dropout_value, args.word_index_dir
    )
