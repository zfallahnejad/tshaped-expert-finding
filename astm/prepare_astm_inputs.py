import os
import re
import random
import pickle
import argparse
import statistics
from collections import Counter

JAVA_TOP_SO_TAGS = [
    "android", "swing", "eclipse", "spring", "hibernate", "arrays", "multithreading", "xml", "jsp", "string",
    "servlets", "maven", "java-ee", "mysql", "spring-mvc", "json", "regex", "tomcat", "jpa", "jdbc", "javascript",
    "arraylist", "web-services", "sql", "generics", "netbeans", "sockets", "user-interface", "jar", "html", "jsf",
    "database", "file", "google-app-engine", "gwt", "junit", "exception", "algorithm", "rest", "class", "performance",
    "applet", "image", "jtable", "c#", "jframe", "collections", "c++", "methods", "oop", "linux",
    "nullpointerexception", "jaxb", "parsing", "oracle", "concurrency", "php", "jpanel", "jboss", "object", "ant",
    "date", "selenium", "javafx", "jvm", "list", "struts2", "hashmap", "sorting", "awt", "http", "inheritance",
    "reflection", "hadoop", "windows", "loops", "unit-testing", "sqlite", "design-patterns", "serialization",
    "security", "intellij-idea", "file-io", "logging", "swt", "apache", "annotations", "jquery", "jersey", "scala",
    "libgdx", "osx", "encryption", "spring-security", "log4j", "python", "jni", "soap", "interface", "io"
]  # "java"
ANDROID_TOP_TAGS = [
    "java", "android-layout", "eclipse", "listview", "android-intent", "android-fragments", "android-activity",
    "sqlite", "xml", "android-listview", "cordova", "json", "google-maps", "android-asynctask", "javascript", "webview",
    "facebook", "android-studio", "layout", "image", "ios", "android-actionbar", "android-emulator", "database",
    "android-edittext", "android-ndk", "multithreading", "bitmap", "button", "textview", "php", "service", "imageview",
    "android-widget", "android-viewpager", "broadcastreceiver", "bluetooth", "opengl-es", "view", "google-play",
    "animation", "html", "nullpointerexception", "gps", "mobile", "camera", "web-services", "audio", "fragment",
    "android-webview", "gridview", "actionbarsherlock", "user-interface", "gradle", "android-service", "string",
    "notifications", "sharedpreferences", "performance", "android-camera", "mysql", "arrays", "iphone", "sockets",
    "video", "c#", "android-manifest", "dialog", "android-sqlite", "arraylist", "sdk", "apk", "android-linearlayout",
    "jquery", "admob", "file", "android-gcm", "adb", "spinner", "c++", "android-animation", "html5", "libgdx",
    "android-canvas", "http", "android-arrayadapter", "sms", "android-imageview", "sql", "tabs", "google-play-services",
    "android-mediaplayer", "scrollview", "xamarin", "push-notification", "api", "css", "location", "adt", "parsing"
]  # "android"
CSHARP_TOP_TAGS = [
    ".net", "asp.net", "wpf", "winforms", "asp.net-mvc", "linq", "entity-framework", "sql", "xaml", "xml", "wcf",
    "sql-server", "multithreading", "javascript", "visual-studio-2010", "visual-studio", "asp.net-mvc-4", "regex",
    "jquery", "vb.net", "asp.net-mvc-3", "windows", "generics", "string", "windows-phone-8", "silverlight",
    "web-services", "json", "windows-phone-7", "datagridview", "gridview", "c#-4.0", "c++", "arrays", "html", "mvvm",
    "list", "excel", "reflection", "linq-to-sql", "database", "nhibernate", "mysql", "unit-testing", "razor",
    "data-binding", "visual-studio-2012", "java", "serialization", "datetime", "events", "xna", "performance", "image",
    "ajax", ".net-4.0", "asynchronous", "dll", "asp.net-web-api", "lambda", "datagrid", "exception", "datatable",
    "ado.net", "oop", "sockets", "listview", "windows-8", "iis", "sql-server-2008", "design-patterns", "user-controls",
    "visual-studio-2008", "inheritance", "class", "combobox", "listbox", "mono", "unity3d", "binding",
    "windows-runtime", "textbox", "dictionary", "webforms", ".net-3.5", "azure", "validation", "delegates", "interface",
    "file", "windows-services", "windows-store-apps", "forms", "linq-to-xml", "algorithm", "android",
    "visual-studio-2013", "sharepoint", "collections", "json.net"
]  # "c#"
random.seed(7)


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


def extract_so_post_info(dataset, so_input_path, top_tags, result_path):
    if not os.path.exists(result_path):
        os.makedirs(result_path)

    user_post, post_user = {}, {}
    post_type, post_score = {}, {}
    post_tags, question_tags = {}, {}
    post_tag_freq, question_tag_freq = {}, {}

    id_regex = re.compile("(?<=Id=\")(?P<Id>.*?)(?=\" )")
    tags_regex = re.compile("(?<=Tags=\")(?P<Tags>.*?)(?=\" )")
    post_type_id_regex = re.compile("(?<=PostTypeId=\")(?P<PostTypeId>.*?)(?=\" )")
    score_regex = re.compile("(?<=Score=\")(?P<Score>.*?)(?=\" )")
    owner_regex = re.compile("(?<=OwnerUserId=\")(?P<OwnerUserId>.*?)(?=\" )")
    with open(so_input_path, encoding='utf8') as posts_file:
        for line in posts_file:
            post_id = id_regex.search(line).group('Id')
            post_type_id = int(post_type_id_regex.search(line).group('PostTypeId'))
            score = int(score_regex.search(line).group('Score'))
            if not owner_regex.search(line):
                continue
            owner_id = int(owner_regex.search(line).group('OwnerUserId'))
            if not tags_regex.search(line):
                continue
            tags = tags_regex.search(line).group('Tags').replace("&lt;", "<").replace("&gt;", ">")[1:-1].split("><")
            tags = [tag for tag in tags if tag in top_tags]
            if tags:
                # print(post_id, post_type_id, tags)
                post_tags[post_id] = tags
                post_score[post_id] = score
                post_user[post_id] = owner_id
                post_type[post_id] = post_type_id
                if owner_id in user_post:
                    user_post[owner_id].append(post_id)
                else:
                    user_post[owner_id] = [post_id]
                for tag in tags:
                    if tag in post_tag_freq:
                        post_tag_freq[tag] += 1
                    else:
                        post_tag_freq[tag] = 1

                if post_type_id == 1:
                    question_tags[post_id] = tags
                    for tag in tags:
                        if tag in question_tag_freq:
                            question_tag_freq[tag] += 1
                        else:
                            question_tag_freq[tag] = 1
    with open(os.path.join(result_path, "sac_{}_post_tags.pkl".format(dataset)), 'wb') as output:
        pickle.dump(post_tags, output)
        print("Number of posts: {}".format(len(post_tags)))
    with open(os.path.join(result_path, "sac_{}_question_tags.pkl".format(dataset)), 'wb') as output:
        pickle.dump(question_tags, output)
        print("Number of question: {}".format(len(question_tags)))
    with open(os.path.join(result_path, "sac_{}_answer_tags.pkl".format(dataset)), 'wb') as output:
        answer_tags = {pid: post_tags[pid] for pid in post_tags if pid not in question_tags}
        pickle.dump(answer_tags, output)
        print("Number of answers: {}".format(len(answer_tags)))
    with open(os.path.join(result_path, "sac_{}_user_post.pkl".format(dataset)), 'wb') as output:
        pickle.dump(user_post, output)
    with open(os.path.join(result_path, "sac_{}_post_user.pkl".format(dataset)), 'wb') as output:
        pickle.dump(post_user, output)
    with open(os.path.join(result_path, "sac_{}_post_type.pkl".format(dataset)), 'wb') as output:
        pickle.dump(post_type, output)
    with open(os.path.join(result_path, "sac_{}_post_score.pkl".format(dataset)), 'wb') as output:
        pickle.dump(post_score, output)

    print("\nPosts:")
    print("Number of posts: {}".format(len(post_tags)))
    tags_count = [len(post_tags[pid]) for pid in post_tags]
    length_count = {l: tags_count.count(l) for l in set(tags_count)}
    print("Average number of tags=", statistics.mean(tags_count))
    print("Median number of tags=", statistics.median(tags_count))
    print("Minimum number of tags={}, number of posts={}".format(min(tags_count), length_count[min(tags_count)]))
    print("Maximum number of tags={}, number of posts={}".format(max(tags_count), length_count[max(tags_count)]))
    max_count = max(length_count.values())
    length_max_count = list(length_count.keys())[list(length_count.values()).index(max_count)]
    print("Maximum frequency={}, with length={}".format(max_count, length_max_count))
    print("Tag frequency in posts:")
    for tag in top_tags:
        print(tag, post_tag_freq.get(tag, 0))

    print("\nQuestions:")
    print("Number of question: {}".format(len(question_tags)))
    tags_count = [len(question_tags[pid]) for pid in question_tags]
    length_count = {l: tags_count.count(l) for l in set(tags_count)}
    print("Average number of tags=", statistics.mean(tags_count))
    print("Median number of tags=", statistics.median(tags_count))
    print("Minimum number of tags={}, number of posts={}".format(min(tags_count), length_count[min(tags_count)]))
    print("Maximum number of tags={}, number of posts={}".format(max(tags_count), length_count[max(tags_count)]))
    max_count = max(length_count.values())
    length_max_count = list(length_count.keys())[list(length_count.values()).index(max_count)]
    print("Maximum frequency={}, with length={}".format(max_count, length_max_count))
    print("Tag frequency in questions:")
    for tag in top_tags:
        print(tag, question_tag_freq.get(tag, 0))

    scores = [post_score[pid] for pid in post_score]
    score_freq = {l: scores.count(l) for l in set(scores)}
    print("Average score=", statistics.mean(scores))
    print("Median score=", statistics.median(scores))
    print("Minimum score={}, number of posts={}".format(min(scores), score_freq[min(scores)]))
    print("Maximum score={}, number of posts={}".format(max(scores), score_freq[max(scores)]))
    max_count = max(score_freq.values())
    length_max_count = list(score_freq.keys())[list(score_freq.values()).index(max_count)]
    print("Maximum frequency={}, with score={}".format(max_count, length_max_count))

    print("number of users: ", len(user_post))
    print("number of posts: ", len(post_score))

    num_users_with_posts_score_gt_th = {
        0: 0, 1: 0, 2: 0, 3: 0, 4: 0, 5: 0, 7: 0, 10: 0, 12: 0, 13: 0, 15: 0, 20: 0, 25: 0
    }
    num_users_with_questions_score_gt_th = {
        0: 0, 1: 0, 2: 0, 3: 0, 4: 0, 5: 0, 7: 0, 10: 0, 12: 0, 13: 0, 15: 0, 20: 0, 25: 0
    }
    num_users_with_answers_score_gt_th = {
        0: 0, 1: 0, 2: 0, 3: 0, 4: 0, 5: 0, 7: 0, 10: 0, 12: 0, 13: 0, 15: 0, 20: 0, 25: 0
    }

    max_num_posts = 1
    max_num_posts_score_ge_th = {0: 0, 1: 0, 2: 0, 3: 0, 4: 0, 5: 0, 7: 0, 10: 0, 12: 0, 13: 0, 15: 0, 20: 0, 25: 0}
    max_num_questions_score_ge_th = {0: 0, 1: 0, 2: 0, 3: 0, 4: 0, 5: 0, 7: 0, 10: 0, 12: 0, 13: 0, 15: 0, 20: 0, 25: 0}
    max_num_answers_score_ge_th = {0: 0, 1: 0, 2: 0, 3: 0, 4: 0, 5: 0, 7: 0, 10: 0, 12: 0, 13: 0, 15: 0, 20: 0, 25: 0}

    for user in user_post:
        max_num_posts = max(max_num_posts, len(user_post[user]))
        for th in max_num_posts_score_ge_th:
            posts_with_score_ge_th = [p for p in user_post[user] if post_score[p] >= th]
            questions_with_score_ge_th = [p for p in user_post[user] if post_type[p] == 1 and post_score[p] >= th]
            answers_with_score_ge_th = [p for p in user_post[user] if post_type[p] == 2 and post_score[p] >= th]

            if posts_with_score_ge_th:
                num_users_with_posts_score_gt_th[th] += 1
            if questions_with_score_ge_th:
                num_users_with_questions_score_gt_th[th] += 1
            if answers_with_score_ge_th:
                num_users_with_answers_score_gt_th[th] += 1

            max_num_posts_score_ge_th[th] = max(max_num_posts_score_ge_th[th], len(posts_with_score_ge_th))
            max_num_questions_score_ge_th[th] = max(max_num_questions_score_ge_th[th], len(questions_with_score_ge_th))
            max_num_answers_score_ge_th[th] = max(max_num_answers_score_ge_th[th], len(answers_with_score_ge_th))

    print("maximum number of post each user have: ", max_num_posts)
    for th in [0, 1, 2, 3, 4, 5, 7, 10, 12, 13, 15, 20, 25]:
        print("We have {} users with posts with score >= {} and "
              "maximum number of these user's posts are: {}".format(num_users_with_posts_score_gt_th[th], th,
                                                                    max_num_posts_score_ge_th[th]))
    for th in [0, 1, 2, 3, 4, 5, 7, 10, 12, 13, 15, 20, 25]:
        print("We have {} users with questions with score >= {} and "
              "maximum number of these user's questions are: {}".format(num_users_with_questions_score_gt_th[th], th,
                                                                        max_num_questions_score_ge_th[th]))
    for th in [0, 1, 2, 3, 4, 5, 7, 10, 12, 13, 15, 20, 25]:
        print("We have {} users with answers with score >= {} and "
              "maximum number of these user's answers are: {}".format(num_users_with_answers_score_gt_th[th], th,
                                                                      max_num_answers_score_ge_th[th]))


def get_samples(dataset, tag_skill, result_path, train_part, post_type):
    print("tags:", set(tag_skill.keys()))
    sample_filename = os.path.join(result_path, "sa_sample_{}_{}s_tr{}.pkl".format(dataset, post_type, train_part))
    if os.path.exists(sample_filename):
        with open(sample_filename, "rb") as input_file:
            sample_post_tags = pickle.load(input_file)
        print("#total sample {}s: {}".format(post_type, len(sample_post_tags)))
        all_tags = [tag for pid, tags in sample_post_tags.items() for tag in tags]
        for t in tag_skill:
            print(t, all_tags.count(t), len(all_tags))
        return sample_filename, sample_post_tags

    with open(os.path.join(result_path, "sac_{}_{}_tags.pkl".format(dataset, post_type)), "rb") as input_file:
        post_tags = pickle.load(input_file)
    print("Number of input {}s: {}".format(post_type, len(post_tags)))

    with open(os.path.join(result_path, "sac_{}_post_score.pkl".format(dataset)), "rb") as input_file:
        post_score = pickle.load(input_file)
        post_score = {pid: post_score[pid] for pid in post_score if pid in post_tags}
    # remove posts with negative score
    post_tags = {pid: tags for pid, tags in post_tags.items() if post_score[pid] >= 0}
    print("Number of input {}s with non-negative score: {}".format(post_type, len(post_tags)))

    tag_posts, tag_count = {t: [] for t in tag_skill}, {t: 0 for t in tag_skill}
    for post_id, tags in post_tags.items():
        for t in tags:
            tag_posts[t].append(post_id)
            tag_count[t] += 1
    print("Number of input {}s with non-negative score and at least one tag: {}".format(
        post_type, len(post_tags)))

    if train_part == 1.0:
        sample_post_tags = post_tags
    else:
        print("sampling posts for each tags:")
        sample_posts = []
        sample_tag_posts, sample_tag_count = {t: [] for t in tag_skill}, {t: 0 for t in tag_skill}
        for tag, count in sorted(tag_count.items(), key=lambda x: x[1]):
            print(tag, count)
            num_samples = int(train_part * len(tag_posts[tag]))
            candidates = [pid for pid in tag_posts[tag] if pid not in sample_posts]
            random.shuffle(candidates)
            sample_posts += candidates[:num_samples]
            for pid in candidates[:num_samples]:
                for t in post_tags[pid]:
                    sample_tag_posts[t].append(pid)
                    sample_tag_count[t] += 1
            print("#samples: {}, remaining: {}".format(
                len(candidates), num_samples - len(candidates) if len(candidates) < num_samples else 0
            ))
        print("sample_tag_count: {}".format(sample_tag_count))
        num_all_samples = int(train_part * len(post_tags))
        if len(sample_posts) < num_all_samples:
            print("#remaining: {}".format(num_all_samples - len(sample_posts)))
            candidates = [pid for pid in post_tags if pid not in sample_posts]
            random.shuffle(candidates)
            sample_posts += candidates[:(num_all_samples - len(sample_posts))]
        elif len(sample_posts) > num_all_samples:
            print("we have {} samples but we need {} samples so we need to ignore some of them!".format(
                len(sample_posts), num_all_samples
            ))
            while len(sample_posts) > num_all_samples:
                tag_with_max_samples = max(sample_tag_count, key=sample_tag_count.get)
                sample_pid = random.choice(sample_tag_posts[tag_with_max_samples])
                for t in post_tags[sample_pid]:
                    sample_tag_posts[t].remove(sample_pid)
                    sample_tag_count[t] -= 1
                sample_posts.remove(sample_pid)
            print("now we have {} samples".format(len(sample_posts)))
            print("sample_tag_count: {}".format(sample_tag_count))
        print("#samples: {}".format(len(sample_posts)))

        sample_post_tags = {p: t for p, t in post_tags.items() if p in sample_posts}

    with open(sample_filename, 'wb') as output:
        pickle.dump(sample_post_tags, output)

    print("#total sample {}s: {}".format(post_type, len(sample_post_tags)))
    all_tags = [tag for pid, tags in sample_post_tags.items() for tag in tags]
    for t in tag_skill:
        print(t, all_tags.count(t), len(all_tags))
    return sample_filename, sample_post_tags


def prepare_inputs(data_path, sample_post_tags, result_path, dataset, post_type, train_part, word_topic_vectors_path,
                   word_vector_dim, num_words):
    all_docs = []
    with open(data_path, encoding="utf8") as infile:
        for line in infile:
            line = line.strip().split()
            post_id = line[0]
            if post_id in sample_post_tags:
                post_text = line[1:]
                all_docs += post_text

    words_freq = Counter(all_docs)
    del all_docs
    print("Number of unique words in this sample dataset: {}".format(len(words_freq)))

    word_index = {}
    for i, (word, freq) in enumerate(words_freq.most_common()):
        word_index[word] = i + 1

    word_vectors = set([])
    with open(os.path.join(word_topic_vectors_path, "word_topic_vectors_{}_{}d.txt".format(dataset, word_vector_dim)),
              encoding="utf8") as infile:
        for line in infile:
            line = line.strip().split('\t')
            word_vectors.add(line[0])
    print('We trained %s word vectors using lda module.' % len(word_vectors))

    j = 1
    absent_words = 0
    new_word_index = {}
    filters = ['!', '"', '#', '$', '%', '&', '(', ')', '*', '+', ',', '-', '.', '/', ':', ';', '<', '=', '>', '?', '@',
               '[', '\\', ']', '^', '_', '`', '{', '|', '}', '~', '\t', '\n']
    for word, i in word_index.items():
        if word in filters:
            continue
        if word in word_vectors:
            new_word_index[word] = j
            j += 1
            if j == num_words:
                break
        else:
            absent_words += 1
    print("Unique Tokens in Training Data: ", len(new_word_index))

    word_index_text_file = os.path.join(
        result_path, "sa_new_word_index_{}_{}s_tr{}_{}d_{}wr.txt".format(
            dataset, post_type, train_part, word_vector_dim, num_words))
    word_index_path = os.path.join(result_path, "sa_word_index_{}_{}s_tr{}_{}d_{}wr.pkl".format(
        dataset, post_type, train_part, word_vector_dim, num_words))
    with open(word_index_text_file, "w", encoding='utf8') as out_file:
        for word, i in new_word_index.items():
            out_file.write(str(i) + "\t" + word + "\n")
    with open(word_index_path, 'wb') as output:
        pickle.dump(new_word_index, output)


def prepare(dataset, data_path, skill_cluster_path, so_input_path, result_path, train_part, post_type,
            word_topic_vectors_path, word_vector_dim, num_words):
    print("dataset:", dataset)
    skill_tags, tag_skill = load_skill_area_tag(skill_cluster_path)
    print("tags:", list(tag_skill.keys()))
    extract_so_post_info(dataset, so_input_path, list(tag_skill.keys()), result_path)
    sample_sa_post_filepath, sample_post_tags = get_samples(dataset, tag_skill, result_path, train_part, post_type)
    prepare_inputs(data_path, sample_post_tags, result_path, dataset, post_type, train_part, word_topic_vectors_path,
                   word_vector_dim, num_words)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--dataset', required=True)
    parser.add_argument('--data_path', help='', required=True)
    parser.add_argument('--skill_cluster_path', required=True)
    parser.add_argument('--so_input_path', required=True)
    parser.add_argument('--result_path', required=True)
    parser.add_argument('--train_part', type=float, help="0.5, 0.75 or 1.0", required=True)
    parser.add_argument('--post_type', help="post, question or answer", required=True)
    parser.add_argument('--num_words', type=int, help="", required=True)
    parser.add_argument('--word_vector_dim', type=int, help="", required=True)
    parser.add_argument('--word_topic_vectors_path', required=True)
    args = parser.parse_args()

    print(
        "python3 prepare.py --dataset {} --data_path {} --skill_cluster_path {} --so_input_path {} --result_path {} "
        "--train_part {} --post_type {} --word_topic_vectors_path {} --num_words {} --word_vector_dim {}".format(
            args.dataset, args.data_path, args.skill_cluster_path, args.so_input_path, args.result_path,
            args.train_part, args.post_type, args.word_topic_vectors_path, args.num_words, args.word_vector_dim)
    )
    prepare(
        dataset=args.dataset,
        data_path=args.data_path,
        skill_cluster_path=args.skill_cluster_path,
        so_input_path=args.so_input_path,
        result_path=args.result_path,
        train_part=args.train_part,
        post_type=args.post_type,
        word_topic_vectors_path=args.word_topic_vectors_path,
        word_vector_dim=args.word_vector_dim,
        num_words=args.num_words
    )
