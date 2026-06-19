#!/bin/bash

rm -rf film-collages
mkdir film-collages
find . -name "*collage.jpg" -not -path "./film-collages/*" -exec cp {} film-collages/ \;
