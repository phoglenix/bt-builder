# trying out some different parameters for GLAM

# echo the commands as they run
# set -v (moved further down)
# quick and dirty:	glam2 -r 1 -n 1000 p my_prots.fa
# slow and thorough:	glam2 -n 1000000 p my_prots.fa

# glam2 -r 1 -n 1000 alphabet-example.txt fasta-input-example.txt
# glam2 -r 1 -n 1000 alphabet-example.txt test_range_fasta.txt
# glam2 -r 1 -n 1000 test_range_alphabet.txt test_range_fasta.txt




# glam2 -n 1000000 -O glam2_out/testing-run1 alphabet_glam.txt bt_seq_encoded.txt

# ## bad -- seems to find sequences of repeats of single characters that don't align at all
# glam2 -q 1 -r 3 -n 10000 -O glam2_out/testing-lowq alphabet_glam.txt bt_seq_encoded.txt
# glam2 -q 1 -I 1 -J 1000 -r 3 -n 100000 -O glam2_out/testing-lowq2 alphabet_glam.txt bt_seq_encoded.txt
# ## end of bad
# glam2 -r 3 -n 10000 -O glam2_out/testing-base alphabet_glam.txt bt_seq_encoded.txt
# glam2 -t 5 -r 3 -n 10000 -O glam2_out/testing-hightemp alphabet_glam.txt bt_seq_encoded.txt
# glam2 -t 5 -c 5 -r 3 -n 10000 -O glam2_out/testing-hightemp-highcool alphabet_glam.txt bt_seq_encoded.txt
# glam2 -t 5 -c 2 -r 3 -n 10000 -O glam2_out/testing-hightemp-medcool alphabet_glam.txt bt_seq_encoded.txt

# glam2 -t 3 -I 1 -J 1000 -r 3 -n 100000 -O glam2_out/testing-medtemp-short alphabet_glam.txt bt_seq_encoded.txt
# glam2 -t 3 -I 1 -J 1000 -r 3 -n 1000000 -O glam2_out/testing-medtemp-longer alphabet_glam.txt bt_seq_encoded.txt

# glam2 -I 1 -J 10 -r 3 -n 10000 -O glam2_out/testing-lessinsertion1 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -I 1 -J 100 -r 3 -n 10000 -O glam2_out/testing-lessinsertion2 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -I 1 -J 1000 -r 3 -n 10000 -O glam2_out/testing-lessinsertion3 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -I 1 -J 10000 -r 3 -n 10000 -O glam2_out/testing-lessinsertion4 alphabet_glam.txt bt_seq_encoded.txt

# glam2 -I 1 -J 1000 -w 10 -b 20 -r 3 -n 10000 -O glam2_out/testing-wider1 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -I 1 -J 1000 -w 50 -b 100 -r 3 -n 10000 -O glam2_out/testing-wider2 alphabet_glam.txt bt_seq_encoded.txt
# ## accuracy loss here:
# glam2 -I 1 -J 10000 -w 75 -b 120 -r 3 -n 10000 -O glam2_out/testing-wider3 alphabet_glam.txt bt_seq_encoded.txt

# glam2 -D 5 -E 200 -I 1 -J 10000 -r 3 -n 10000 -O glam2_out/testing-lessdeletion1 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -D 5 -E 2000 -I 1 -J 10000 -r 3 -n 10000 -O glam2_out/testing-lessdeletion2 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -D 5 -E 20000 -I 1 -J 10000 -r 3 -n 10000 -O glam2_out/testing-lessdeletion3 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -D 5 -E 200000 -I 1 -J 10000 -r 3 -n 10000 -O glam2_out/testing-lessdeletion4 alphabet_glam.txt bt_seq_encoded.txt

# ## default values: D: 0.1, E: 2, (x20), I: 0.02, J: 1, (x50)
# glam2 -D 5 -E 200 -I 1 -J 10000 -r 3 -n 100000 -O glam2_out/testing-lessindel1 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -D 5 -E 2000 -I 1 -J 10000 -r 3 -n 100000 -O glam2_out/testing-lessindel2 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -D 5 -E 20000 -I 1 -J 10000 -r 3 -n 100000 -O glam2_out/testing-lessindel3 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -D 5 -E 200000 -I 1 -J 10000 -r 3 -n 100000 -O glam2_out/testing-lessindel4 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -D 5 -E 200 -I 1 -J 4000 -r 3 -n 100000 -O glam2_out/testing-lessindel5 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -D 5 -E 200 -I 1 -J 10 -r 3 -n 100000 -O glam2_out/testing-lessindel5.5 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -D 10 -E 200 -I 1 -J 4000 -r 3 -n 10000 -O glam2_out/testing-lessindel6 alphabet_glam.txt bt_seq_encoded.txt
#### more insertions but not really any better
# glam2 -D 10 -E 200 -I 10 -J 4000 -r 3 -n 10000 -O glam2_out/testing-lessindel7 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -D 1000 -E 20000 -I 100 -J 400000 -r 3 -n 10000 -O glam2_out/testing-lessindel6.1 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -D 100000 -E 2000000 -I 10000 -J 40000000 -r 3 -n 10000 -O glam2_out/testing-lessindel6.2 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -D 1e5 -E 2e6 -I 1e4 -J 4e7 -r 3 -n 10000 -O glam2_out/testing-lessindel6.2.1 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -D 1e7 -E 2e8 -I 1e6 -J 4e9 -r 3 -n 10000 -O glam2_out/testing-lessindel6.3 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -D 1e17 -E 2e18 -I 1e16 -J 4e19 -r 3 -n 10000 -O glam2_out/testing-lessindel6.4 alphabet_glam.txt bt_seq_encoded.txt
#### More insertions and maybe worse
# glam2 -D 100000 -E 2000000 -I 50000 -J 40000000 -r 3 -n 10000 -O glam2_out/testing-lessindel6.2.2 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -D 500000 -E 2000000 -I 10000 -J 40000000 -r 3 -n 10000 -O glam2_out/testing-lessindel6.2.3 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -D 500000 -E 2000000 -I 10000 -J 40000000 -r 3 -n 10000 -O glam2_out/testing-best1 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -w 5 -b 15 -D 500000 -E 2000000 -I 10000 -J 40000000 -r 3 -n 10000 -O glam2_out/testing-best2 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -b 30 -D 500000 -E 2000000 -I 10000 -J 40000000 -r 3 -n 100000 -O glam2_out/testing-best3 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -b 30 -D 500000 -E 2000000 -I 20000 -J 40000000 -r 3 -n 10000 -O glam2_out/testing-best4 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -b 30 -D 500000 -E 2000000 -I 40000 -J 40000000 -r 3 -n 10000 -O glam2_out/testing-best5 alphabet_glam.txt bt_seq_encoded.txt
# glam2 -b 30 -D 500000 -E 2000000 -I 100000 -J 40000000 -r 3 -n 10000 -O glam2_out/testing-best6-worse alphabet_glam.txt bt_seq_encoded.txt

set -v
glam2 -b 30 -D 500000 -E 2000000 -I 40000 -J 40000000 -r 3 -n 10000 -O glam2_out/run1 alphabet_glam.txt bt_seq_encoded.txt
