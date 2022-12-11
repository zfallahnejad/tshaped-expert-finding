import os
import re
import html

so_input_path = "./data/Posts.xml"
so_output_path = "./data/"
if not os.path.exists(so_output_path):
    os.makedirs(so_output_path)

for target_tag, outfile_name in [("java", "JavaPosts.xml"), ("android", "AndroidPosts.xml"), ("c#", "C#Posts.xml")]:
    print(target_tag, outfile_name)
    questions = set()
    id_regex = re.compile("(?<=Id=\")(?P<Id>.*?)(?=\" )")
    post_type_id_regex = re.compile("(?<=PostTypeId=\")(?P<PostTypeId>.*?)(?=\" )")
    parent_id_regex = re.compile("(?<=ParentId=\")(?P<ParentId>.*?)(?=\" )")
    tags_regex = re.compile("(?<=Tags=\")(?P<Tags>.*?)(?=\" )")
    print("Loading posts in order to find question ids...")
    with open(so_input_path, encoding='utf8') as posts_file:
        for line in posts_file:
            if "<row" not in line:
                continue
            post_id = id_regex.search(line).group('Id')
            if post_type_id_regex.search(line):
                post_type_id = int(post_type_id_regex.search(line).group('PostTypeId'))
            else:
                post_type_id = -1

            if tags_regex.search(line):
                tags = tags_regex.search(line).group('Tags').replace("&lt;", "<").replace("&gt;", ">")[1:-1].split("><")
            else:
                tags = []

            if post_type_id == 1 and target_tag in tags:
                questions.add(post_id)
    print("number of {} questions: {}".format(target_tag, len(questions)))

    answers = 0
    with open(so_output_path + outfile_name, "w", encoding='utf8') as out_file:
        with open(so_input_path, encoding='utf8') as posts_file:
            for line in posts_file:
                if "<row" not in line:
                    continue
                post_id = id_regex.search(line).group('Id')
                if post_type_id_regex.search(line):
                    post_type_id = int(post_type_id_regex.search(line).group('PostTypeId'))
                else:
                    post_type_id = -1

                if parent_id_regex.search(line):
                    parent_id = parent_id_regex.search(line).group('ParentId')
                else:
                    parent_id = -1

                if post_type_id == 1 and post_id in questions:
                    out_file.write(line)
                elif post_type_id == 2 and parent_id in questions:
                    out_file.write(line)
                    answers += 1
    print("number of {} answers: {}".format(target_tag, answers))


for target_tag, outfile_name, temp_file_name in [("java", "JavaPosts.xml", "JavaPosts_2.xml"),
                                                 ("php", "PhpPosts.xml", "PhpPosts_2.xml")]:
    os.rename(os.path.join(so_output_path, outfile_name), os.path.join(so_output_path, temp_file_name))

    question_tags = {}
    id_regex = re.compile("(?<=Id=\")(?P<Id>.*?)(?=\" )")
    tags_regex = re.compile("(?<=Tags=\")(?P<Tags>.*?)(?=\" )")
    parent_id_regex = re.compile("(?<=ParentId=\")(?P<ParentId>.*?)(?=\" )")
    post_type_id_regex = re.compile("(?<=PostTypeId=\")(?P<PostTypeId>.*?)(?=\" )")
    with open(os.path.join(so_output_path, temp_file_name), encoding='utf8') as posts_file:
        for line in posts_file:
            if "<row" not in line:
                continue
            post_id = id_regex.search(line).group('Id')
            if post_type_id_regex.search(line):
                post_type_id = int(post_type_id_regex.search(line).group('PostTypeId'))
            else:
                continue
            if post_type_id == 1:
                question_tags[post_id] = tags_regex.search(line).group('Tags')

    with open(os.path.join(so_output_path, outfile_name), "w", encoding='utf8') as out_file:
        with open(os.path.join(so_output_path, temp_file_name), encoding='utf8') as posts_file:
            for line in posts_file:
                if "<row" not in line:
                    continue
                line = line.strip()
                post_id = id_regex.search(line).group('Id')
                if post_type_id_regex.search(line):
                    post_type_id = int(post_type_id_regex.search(line).group('PostTypeId'))
                    if post_type_id == 2:
                        if parent_id_regex.search(line):
                            parent_id = parent_id_regex.search(line).group('ParentId')
                            if parent_id in question_tags:
                                line = re.sub(r'/>$', "Tags=\"{}\" />\n".format(question_tags[parent_id]), line).strip()
                                out_file.write(line + "\n")
                            else:
                                out_file.write(line + "\n")
                        else:
                            out_file.write(line + "\n")
                    else:
                        out_file.write(line + "\n")
                else:
                    out_file.write(line + "\n")

for target_tag, infile_name in [("java", "JavaPosts.xml"), ("android", "AndroidPosts.xml"), ("c#", "C#Posts.xml")]:
    so_input_path = "./data/" + infile_name
    so_output_path = "./data/"
    if not os.path.exists(so_output_path):
        os.makedirs(so_output_path)

    pattern_1 = re.compile("[a-zA-Z\d][,|.|?|:]\s")

    Id_regex = re.compile("(?<=Id=\")(?P<Id>.*?)(?=\" )")
    Body_regex = re.compile("(?<=Body=\")(?P<Body>.*?)(?=\" )")
    print("Preprocessing input text...")
    with open(so_output_path + "so_{}.txt".format(target_tag), "w", encoding='utf8') as post_out_file:
        with open(so_input_path, encoding='utf8') as posts_file:
            for line in posts_file:
                post_id = Id_regex.search(line).group('Id')
                print(post_id)
                body = Body_regex.search(line).group('Body')
                doc = body.lower().replace('&amp;', '&').replace("&lt;", "<").replace("&gt;", ">").replace("&#xa;", " ")
                doc = html.unescape(doc)
                for tag in ['<p>', '</p>', '<b>', "<br>", "<br/>", "<br />", "<ul>", "</ul>", "<li>", "</li>", "<dt>",
                            "<ol>", "</ol>", "<hr>", "<i>", "</i>", "<b>", "</b>", "<pre>", "</pre>", "<blockquote>",
                            "</blockquote>", "<code>", "</code>", "<hr>", "<hr/>", "<em>", "</em>", "</a>", "<strong>",
                            "</strong>", "<strike>", "<t>", "<h1>", "</h1>", "<h2>", "</h2>", "<h3>", "</h3>", "<h4>",
                            "</h4>", "<h5>", "</h5>", "<h6>", "</h6>", "<string>", "<html>", "</html>", "<head>",
                            "<dl>", "</dl>", "<dd>", "<ul>", "</ul>", "<hr />", "<table>", "</table>", "<tr>", "</tr>",
                            "<td>", "</td>", "<th>", "</th>", "<frameset>", "</frameset>", "</frame>", "<option>",
                            "<noframes>", "</noframes>", "<form>", "</form>", "</select>", "<body>", "</body>",
                            "</head>", "<tt>", "</tt>", "<cite>", "</cite>", "</font>"]:
                    doc = ' {} '.format(tag).join(doc.split(tag))
                for tag in ['<a ', '<img ', '<body ', '<font ', '<p ', "<input ", '<div ', '<hr ', "<select ",
                            '<table ', '<td ', '<tr ', '<frame ', '<frameset ']:
                    doc = ' {}'.format(tag).join(doc.split(tag))
                tokens = doc.split()
                for stop_word in ['.', ',', '?', ':']:
                    if stop_word in tokens:
                        tokens.remove(stop_word)
                doc = ' '.join(tokens)
                doc = doc.replace("\n", " ").replace("\r", " ")

                search_results = []
                for m in pattern_1.finditer(doc):
                    search_results.insert(0, m.span())
                for s in search_results:
                    doc = doc[:s[0] + 1] + " " + doc[s[1]:]

                post_out_file.write(str(post_id) + ' ' + doc + "\n")
    print("End!")
