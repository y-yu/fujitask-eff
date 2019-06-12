package repository

import fujitask.eff.Fujitask.Transaction

trait ReadTransaction extends Transaction

trait ReadWriteTransaction extends ReadTransaction