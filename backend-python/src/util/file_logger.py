import os

if os.path.exists("log.txt"):
    os.remove("log.txt")


def write_log(text: str):
    with open("log.txt", "w") as log_file:
        log_file.write(text + "\n")
