# Top k-routes with preferences

### References

This project is completely based on the following work:
*"Top-k Route Search through Submodularity Modeling of Recurrent POI Features"* by *Hongwei Liang* and *Ke Wang*.

Links to the original work:  
1. [Paper of Top-k route search](https://arxiv.org/) The original algorithm paper.
2. [Hop Doubling Label Indexing for Point-to-Point Distance Querying on Scale-Free Networks](http://www.vldb.org/pvldb/vol7/p1203-jiang.pdf)
 paper, that is frequently referenced. Its algorithm is used inside the top-k search.  
3. [github](https://github.com/LazyAir/SIGIR18) of the original work. (Realised in `C++`)

### Description

This is my coursework for the first term of *Algorithms and data structures* course at university.  
The aim of it is to reproduce the results from the original paper.  
I'm implementing the algorithm in `Java`

### Structure & functions
##### Folders
`data` - folder, containing all the data.

All the code is inside the `src/main/java` folder:  
`offline` - folder, containing the *2-hop label indexing* algorithm. It has a single class responsible for the generation
of labels. It's main function is `processEdges` which takes 2 arguments - `pathFrom` and `pathTo`, which are the source
and target files.  

`online` - folder, containing objects, responsible for retrieving all the required data for *top-k route search* algorithm.
It contains `FeaturesFrame`, `TwoHopFrame` and `WeightFrame` responsible for opening and processing of the
vertex-feature, two-hop and stay-time tables accordingly.

##### Dependencies
Project uses `Apache Spark` for faster data retreival and processing.
