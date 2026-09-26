package com.electrical.calculationspro.data

import java.util.ArrayDeque

/**
 * SLD TOPOLOGY ENGINE
 *
 * مسؤول عن تحديد اتجاه الشبكة كهربائيًا:
 *
 * SOURCE
 *   ↓
 * TRANSFORMER
 *   ↓
 * PANEL
 *   ↓
 * LOAD
 *
 * لا يعتمد الحساب على اتجاه الخط الذي رسمه المستخدم.
 *
 * يتم تحديد المصدر أولاً، ثم يتم توجيه كل connection
 * من المصدر إلى الأحمال downstream.
 */
object SldTopologyEngine {

    data class Topology(
        val source: SldNode,
        val connections: List<SldConnection>,
        val children: Map<String, List<SldNode>>,
        val parents: Map<String, List<SldNode>>
    )

    fun build(
        network: SldNetwork
    ): Topology {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        val nodeMap =
            network.nodes.associateBy { it.id }

        require(
            nodeMap.size == network.nodes.size
        ) {
            "Duplicate SLD node IDs."
        }

        network.connections.forEach { connection ->

            require(
                connection.fromNodeId in nodeMap
            ) {
                "Connection ${connection.id}: source node does not exist."
            }

            require(
                connection.toNodeId in nodeMap
            ) {
                "Connection ${connection.id}: destination node does not exist."
            }

            require(
                connection.fromNodeId != connection.toNodeId
            ) {
                "Connection ${connection.id}: node cannot connect to itself."
            }
        }

        val source =
            network.nodes.firstOrNull {
                it.type == SldNodeType.SOURCE
            }
                ?: network.nodes.firstOrNull {
                    network.connections.none { connection ->
                        connection.toNodeId == it.id
                    }
                }
                ?: network.nodes.first()

        /*
         * Build an undirected graph only for topology discovery.
         *
         * The stored from/to direction is NOT trusted here.
         */
        val neighbours =
            mutableMapOf<String, MutableList<String>>()

        network.nodes.forEach {
            neighbours[it.id] = mutableListOf()
        }

        network.connections.forEach { connection ->

            neighbours[
                connection.fromNodeId
            ]?.add(
                connection.toNodeId
            )

            neighbours[
                connection.toNodeId
            ]?.add(
                connection.fromNodeId
            )
        }

        /*
         * BFS from electrical source.
         *
         * distance 0 = source
         * distance 1 = first downstream level
         * distance 2 = second downstream level
         */
        val distance =
            mutableMapOf<String, Int>()

        val queue =
            ArrayDeque<String>()

        distance[source.id] = 0
        queue.add(source.id)

        while (queue.isNotEmpty()) {

            val current =
                queue.removeFirst()

            val currentDistance =
                distance[current] ?: 0

            neighbours[current]
                .orEmpty()
                .forEach { next ->

                    if (next !in distance) {

                        distance[next] =
                            currentDistance + 1

                        queue.add(next)
                    }
                }
        }

        /*
         * A professional electrical SLD calculation cannot
         * silently calculate a disconnected element.
         */
        val disconnected =
            network.nodes
                .filter {
                    it.id !in distance
                }

        require(disconnected.isEmpty()) {

            "Disconnected SLD nodes: " +
                disconnected.joinToString {
                    it.name
                }
        }

        /*
         * Orient every connection from lower BFS level
         * toward higher BFS level.
         */
        val orientedConnections =
            network.connections.map { connection ->

                val fromDistance =
                    distance[
                        connection.fromNodeId
                    ]!!

                val toDistance =
                    distance[
                        connection.toNodeId
                    ]!!

                when {

                    fromDistance < toDistance ->
                        connection

                    toDistance < fromDistance ->
                        connection.copy(
                            fromNodeId =
                                connection.toNodeId,
                            toNodeId =
                                connection.fromNodeId
                        )

                    else ->
                        throw IllegalArgumentException(
                            "Invalid SLD topology: " +
                                "connection ${connection.id} " +
                                "creates an ambiguous/cyclic path."
                        )
                }
            }

        /*
         * Detect multiple upstream parents.
         *
         * A normal radial LV SLD should have one electrical
         * parent per node.
         */
        val parents =
            mutableMapOf<
                String,
                MutableList<String>
            >()

        orientedConnections.forEach { connection ->

            parents
                .getOrPut(
                    connection.toNodeId
                ) {
                    mutableListOf()
                }
                .add(
                    connection.fromNodeId
                )
        }

        val multiParentNodes =
            parents.filter {
                it.value.size > 1
            }

        require(
            multiParentNodes.isEmpty()
        ) {
            "Invalid radial SLD: node(s) have multiple upstream feeders: " +
                multiParentNodes.keys.joinToString()
        }

        val children =
            mutableMapOf<
                String,
                MutableList<SldNode>
            >()

        val finalParents =
            mutableMapOf<
                String,
                MutableList<SldNode>
            >()

        network.nodes.forEach {
            children[it.id] =
                mutableListOf()

            finalParents[it.id] =
                mutableListOf()
        }

        orientedConnections.forEach { connection ->

            val from =
                nodeMap[
                    connection.fromNodeId
                ]!!

            val to =
                nodeMap[
                    connection.toNodeId
                ]!!

            children[from.id]!!
                .add(to)

            finalParents[to.id]!!
                .add(from)
        }

        return Topology(
            source =
                source,

            connections =
                orientedConnections,

            children =
                children.mapValues {
                    it.value.toList()
                },

            parents =
                finalParents.mapValues {
                    it.value.toList()
                }
        )
    }
}
