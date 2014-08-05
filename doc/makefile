.PHONY: diagrams all

all: diagrams ../README.html

diagrams: $(patsubst %.uml,%.png,$(wildcard *.uml))

../README.html: ../README.md
	cd .. && \
	pandoc -S -s \
	-f markdown_github \
	--self-contained --toc -t html -o README.html \
	--default-image-extension=png  \
	README.md 

%.png: %.uml
	java -jar plantuml.jar $<