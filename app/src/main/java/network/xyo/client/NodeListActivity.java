package network.xyo.client;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import java.util.List;

import network.xyo.sdk.nodes.Archivist;
import network.xyo.sdk.nodes.Bridge;
import network.xyo.sdk.nodes.Node;
import network.xyo.sdk.nodes.Sentinel;

/**
 * An activity representing a list of Nodes. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link NodeDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class NodeListActivity extends AppCompatActivity {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.node_list_activity);

        Node.init(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Unable to Add Node", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        if (findViewById(R.id.node_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        View recyclerView = findViewById(R.id.node_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(this, Node.get(), mTwoPane));
    }

    public static class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final NodeListActivity mParentActivity;
        private final List<Node> mValues;
        private final boolean mTwoPane;
        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Node node = (Node) view.getTag();
                if (mTwoPane) {
                    Bundle arguments = new Bundle();
                    arguments.putString(NodeDetailFragment.NODE_NAME, node.getName());
                    NodeDetailFragment fragment = new NodeDetailFragment();
                    fragment.setArguments(arguments);
                    mParentActivity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.node_detail_container, fragment)
                            .commit();
                } else {
                    Context context = view.getContext();
                    Intent intent = new Intent(context, NodeDetailActivity.class);
                    intent.putExtra(NodeDetailFragment.NODE_NAME, node.getName());

                    context.startActivity(intent);
                }
            }
        };

        SimpleItemRecyclerViewAdapter(NodeListActivity parent,
                                      List<Node> items,
                                      boolean twoPane) {
            mValues = items;
            mParentActivity = parent;
            mTwoPane = twoPane;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.node_list_content, parent, false);
            return new ViewHolder(view);
        }

        private void setInCount(final View view, final Node node) {
            mParentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final TextView textView = view.findViewById(R.id.in);
                    textView.setAlpha(0.0f);
                    textView.setText("In: " + node.totalInCount);
                    textView.animate().alpha(1.0f).setDuration(1000);
                }
            });
        }

        private void setOutCount(final View view, final Node node) {
            mParentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final TextView textView = view.findViewById(R.id.out);
                    textView.setAlpha(0.0f);
                    textView.setText("Out: " + node.totalOutCount);
                    textView.animate().alpha(1.0f).setDuration(1000);
                }
            });
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {

            Node node = mValues.get(position);

            holder.mTitle.setText(node.getName());
            holder.mBody.setText(node.toString());

            this.setInCount(holder.itemView, node);
            this.setOutCount(holder.itemView, node);

            if (node instanceof Sentinel) {
                ((Sentinel) node).setListener(new Sentinel.Listener() {
                    @Override
                    public void locationUpdated(final Sentinel sentinel, final Location location) {
                        mParentActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final ImageView locationView = holder.itemView.findViewById(R.id.location);
                                locationView.setAlpha(0.0f);
                                locationView.animate().alpha(1.0f).setDuration(1000);
                            }
                        });
                    }

                    @Override
                    public void updated() {

                    }

                    @Override
                    public void in(final Node node, final byte[] bytes) {
                        setInCount(holder.itemView, node);
                    }

                    @Override
                    public void out(final Node node, final byte[] bytes) {
                        setOutCount(holder.itemView, node);
                    }
                });
                ((Sentinel) node).pollLocation();
            } else if (node instanceof Bridge) {
                ((Bridge) node).setListener(new Bridge.Listener() {
                    @Override
                    public void in(final Node node, final byte[] bytes) {
                        setInCount(holder.itemView, node);
                    }

                    @Override
                    public void out(final Node node, final byte[] bytes) {
                        setOutCount(holder.itemView, node);
                    }

                    @Override
                    public void updated() {

                    }
                });
            } else if (node instanceof Archivist) {
                ((Archivist) node).setListener(new Archivist.Listener() {
                    @Override
                    public void in(final Node node, final byte[] bytes) {
                        setInCount(holder.itemView, node);
                    }

                    @Override
                    public void out(final Node node, final byte[] bytes) {
                        setOutCount(holder.itemView, node);
                    }

                    @Override
                    public void updated() {

                    }
                });
            }

            holder.itemView.setTag(mValues.get(position));
            holder.itemView.setOnClickListener(mOnClickListener);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mTitle;
            final TextView mBody;

            ViewHolder(View view) {
                super(view);
                mTitle = (TextView) view.findViewById(R.id.title);
                mBody = (TextView) view.findViewById(R.id.body);
            }
        }
    }
}
