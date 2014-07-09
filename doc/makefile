.PHONY: clean

index.html: src/*.md $(patsubst %.uml,%.png,$(wildcard src/*.uml))
	cd src && \
	pandoc -S -s \
	-f markdown_github+pandoc_title_block+implicit_header_references+auto_identifiers\
	+multiline_tables \
	--chapters --self-contained --toc -t html -o ../index.html \
	--default-image-extension=png  \
	backup.md \

clean: 
	rm -f src/*.png
	
%.png: %.uml
	java -jar plantuml.jar $<