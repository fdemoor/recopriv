import matplotlib.pyplot as plt
import numpy as np
from os import getcwd, chdir
import sys

BASEDIR = getcwd()

path = BASEDIR + '/../output/jester/aboveThres/'
filename = 'aboveThres.csv'
filename2 = 'sybilAttack.csv'

N = 24983

AUEXTRA = 0.1

PARSE = True


if PARSE:
    
    EXTRA = [[] for i in range(N)]
    REF = []
    EXPN = []
    SYBN = []
    TIN = []
    currentID = -1;
    
    f = open(path + filename, 'r')
    for line in f:
        info = line.split(',')
        if (int(info[0]) != currentID):
            currentID = int(info[0])
            REF.append(int(info[3]))
        EXTRA[currentID].append(int(info[2]))
    f.close()
    
    #===========================================================================
    # f = open(path + filename2, 'r')
    # for line in f:
    #     info = line.split(',')
    #     EXPN.append(float(info[4]))
    #     SYBN.append(float(info[5]))
    #     TIN.append(float(info[6]))
    # f.close()
    #===========================================================================

    REF = [ (1-AUEXTRA) * e for e in REF]
    
    EXTRA_NP = np.array([ np.array([e for e in L]) for L in EXTRA])
    EXTRA_STATS = [np.median(L) for L in EXTRA_NP]
    #EXTRA_STATS = [np.percentile(L, 75) for L in EXTRA_NP]
    RATE = [REF[i] - EXTRA_STATS[i] for i in range(len(REF))]
    
    f = open(path + 'stats.py', 'w')
    for e in [(RATE,'RATE'), (REF,'REF'), (EXPN,'EXP'), (SYBN,'SYBN'), (TIN,'TIN')]:
        f.write(e[1] + ' = ' + str(e[0]) + '\n')
    f.close()
    
else:
    sys.path.append(path)
    from stats import *


def plotAll():
  
  XMIN = 1
  H = 20
  
  while XMIN < N:
    
    XMAX = min(XMIN + H, N)

    P = [i for i in range(XMIN, XMAX+1)]
    
    fig, ax1 = plt.subplots()
    ax1.boxplot(EXTRA[XMIN-1:XMAX], positions=P)
    ax1.plot(P, REF[XMIN-1:XMAX], c='#FF8C00')

    ax2 = ax1.twinx()
    #ax2.plot(P, EXPN[XMIN-1:XMAX], c='#1E90FF')
    #ax2.plot(P, SYBN[XMIN-1:XMAX], c='#008000')
    ax2.plot(P, TIN[XMIN-1:XMAX], c='#B22222')

    for tick in ax1.get_xticklabels():
      tick.set_rotation(70)

    ax1.set_xlabel('Target ID')

    ax1.set_ylabel('| u - t |')
    ax2.set_ylabel('Target is Neighbor')
    ax2.set_yticks(np.arange(0, 1.01, 0.1))

    ax1.grid()
    
    plt.savefig(path + str(XMIN) + '-' + str(XMAX) + '.svg', bbox_inches='tight')
    
    plt.close()
    
    XMIN = XMIN + H
    
    
def plotScatter():
  
  #plt.scatter(RATE, EXPN, c='#1E90FF', label='expN')
  plt.scatter(RATE, SYBN, c='#008000', label='sybN')
  #plt.scatter(RATE, TIN, c='#B22222', label = 'TiN')
  
  XMIN, XMAX = min(RATE) - 1, max(RATE) + 1
  
  plt.axis([XMIN, XMAX, 0 - 0.1, 1 + 0.1])

  plt.grid()
  
  #plt.ylabel('Target is Neighbor')
  #plt.ylabel('Expected Neighborhoods')
  plt.ylabel('Sybil Neighbors')
  plt.xlabel('x')
  
  #plt.legend(loc=1)
  
  plt.savefig(path + 'scatter.svg', bbox_inches='tight')
  plt.close()
  
  
## ----- ##

#plotScatter()
print('x < 0:', len([e for e in RATE if e < 0]))
