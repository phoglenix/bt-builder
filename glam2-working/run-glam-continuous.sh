set -v

for i in `seq 1 50`;
do
    while [ ! -f glam2_out/glam_processing.flag ]
    do
      sleep 4
    done
    echo Starting processing $i
    glam2 -a 2 -b 20 -w 15 -D 500000 -E 2000000 -I 10000 -J 40000000 -r 3 -n 50000 -O glam2_out/run$i alphabet_glam.txt bt_seq_encoded$i.txt
    rm glam2_out/glam_processing.flag
    echo Finished processing $i
    sleep 4
done  
